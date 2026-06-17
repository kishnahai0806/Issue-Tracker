package com.krish.issuetracker.websocket;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.security.jwt.JwtService;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String PROJECT_TOPIC_PREFIX = "/topic/projects/";
	private static final String PROJECT_TOPIC_SUFFIX = "/issues";
	private static final String USER_DESTINATION_PREFIX = "/user/";
	private static final String ORGANIZATION_TARGET_TYPE = "ORGANIZATION";
	private static final String SUBSCRIBE_ROLE = "REPORTER";

	private final JwtService jwtService;
	private final OrganizationMemberPermissionEvaluator permissionEvaluator;
	private final ProjectRepository projectRepository;
	private final AtomicInteger wsActiveConnections;

	public JwtChannelInterceptor(
			JwtService jwtService,
			OrganizationMemberPermissionEvaluator permissionEvaluator,
			ProjectRepository projectRepository,
			AtomicInteger wsActiveConnections) {
		this.jwtService = jwtService;
		this.permissionEvaluator = permissionEvaluator;
		this.projectRepository = projectRepository;
		this.wsActiveConnections = wsActiveConnections;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			handleConnect(accessor);
		}
		if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
			handleSubscribe(accessor);
		}
		if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
			wsActiveConnections.updateAndGet(current -> Math.max(0, current - 1));
		}

		return message;
	}

	private void handleConnect(StompHeaderAccessor accessor) {
		String token = extractBearerToken(accessor.getFirstNativeHeader(AUTHORIZATION_HEADER))
				.orElseThrow(() -> new MessageDeliveryException("Unauthorized"));

		if (!jwtService.validateToken(token)) {
			throw new MessageDeliveryException("Unauthorized");
		}

		String userId = jwtService.extractUserId(token);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				userId,
				null,
				List.of());
		accessor.setUser(authentication);
		wsActiveConnections.incrementAndGet();
	}

	private void handleSubscribe(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		if (!StringUtils.hasText(destination) || destination.startsWith(USER_DESTINATION_PREFIX)) {
			return;
		}

		if (!isProjectIssueTopic(destination)) {
			return;
		}

		UUID projectId = extractProjectId(destination)
				.orElseThrow(() -> new MessageDeliveryException("Access denied to topic: " + destination));
		Authentication authentication = authentication(accessor.getUser())
				.orElseThrow(() -> new MessageDeliveryException("Unauthorized"));

		if (!hasProjectTopicAccess(authentication, projectId)) {
			throw new MessageDeliveryException("Access denied to topic: " + destination);
		}
	}

	private boolean hasProjectTopicAccess(Authentication authentication, UUID projectId) {
		Optional<Project> project = projectRepository.findById(projectId);
		if (project.isEmpty() || project.get().isArchived()) {
			return false;
		}

		return permissionEvaluator.hasPermission(
				authentication,
				project.get().getOrganizationId(),
				ORGANIZATION_TARGET_TYPE,
				SUBSCRIBE_ROLE);
	}

	private Optional<Authentication> authentication(Principal principal) {
		if (principal instanceof Authentication authentication) {
			return Optional.of(authentication);
		}
		return Optional.empty();
	}

	private Optional<String> extractBearerToken(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return Optional.empty();
		}
		return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()));
	}

	private boolean isProjectIssueTopic(String destination) {
		return destination.startsWith(PROJECT_TOPIC_PREFIX) && destination.endsWith(PROJECT_TOPIC_SUFFIX);
	}

	private Optional<UUID> extractProjectId(String destination) {
		String projectId = destination.substring(
				PROJECT_TOPIC_PREFIX.length(),
				destination.length() - PROJECT_TOPIC_SUFFIX.length());
		try {
			return Optional.of(UUID.fromString(projectId));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
