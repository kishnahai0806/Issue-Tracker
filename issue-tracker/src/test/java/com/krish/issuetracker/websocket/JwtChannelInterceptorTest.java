package com.krish.issuetracker.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.repository.ProjectRepository;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.TokenBlacklist;
import com.krish.issuetracker.security.jwt.JwtService;
import com.krish.issuetracker.security.permission.OrganizationMemberPermissionEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private TokenBlacklist tokenBlacklist;

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrganizationMemberPermissionEvaluator permissionEvaluator;

	@Mock
	private ProjectRepository projectRepository;

	@Test
	void preSend_shouldAuthenticateActiveUserOnConnect() {
		String token = "access-token";
		UUID userId = UUID.randomUUID();
		AtomicInteger activeConnections = new AtomicInteger();
		JwtChannelInterceptor interceptor = interceptor(activeConnections);
		User user = new User();
		user.setId(userId);
		user.setActive(true);
		when(jwtService.validateToken(token)).thenReturn(true);
		when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
		when(jwtService.extractUserId(token)).thenReturn(userId.toString());
		when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.of(user));

		Message<?> result = interceptor.preSend(connectMessage(token), null);
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

		assertThat(accessor).isNotNull();
		assertThat(accessor.getUser()).isNotNull();
		assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
		assertThat(activeConnections).hasValue(1);
	}

	@Test
	void preSend_shouldRejectBlacklistedTokenOnConnect() {
		String token = "access-token";
		AtomicInteger activeConnections = new AtomicInteger();
		JwtChannelInterceptor interceptor = interceptor(activeConnections);
		when(jwtService.validateToken(token)).thenReturn(true);
		when(tokenBlacklist.isBlacklisted(token)).thenReturn(true);

		assertThatThrownBy(() -> interceptor.preSend(connectMessage(token), null))
				.isInstanceOf(MessageDeliveryException.class);
		assertThat(activeConnections).hasValue(0);
	}

	@Test
	void preSend_shouldRejectDisabledUserOnConnect() {
		String token = "access-token";
		UUID userId = UUID.randomUUID();
		AtomicInteger activeConnections = new AtomicInteger();
		JwtChannelInterceptor interceptor = interceptor(activeConnections);
		when(jwtService.validateToken(token)).thenReturn(true);
		when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
		when(jwtService.extractUserId(token)).thenReturn(userId.toString());
		when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> interceptor.preSend(connectMessage(token), null))
				.isInstanceOf(MessageDeliveryException.class);
		assertThat(activeConnections).hasValue(0);
	}

	@Test
	void preSend_shouldAllowSubscribeToAuthorizedProjectIssueTopic() {
		UUID projectId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		Project project = new Project();
		project.setId(projectId);
		project.setOrganizationId(organizationId);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				UUID.randomUUID().toString(),
				null);
		JwtChannelInterceptor interceptor = interceptor(new AtomicInteger());
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setUser(authentication);
		accessor.setDestination("/topic/projects/" + projectId + "/issues");
		Message<?> message = message(accessor);
		when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
		when(permissionEvaluator.hasPermission(authentication, organizationId, "ORGANIZATION", "REPORTER"))
				.thenReturn(true);

		assertThat(interceptor.preSend(message, null)).isSameAs(message);
	}

	@Test
	void preSend_shouldRejectSubscribeToUnknownTopicDestination() {
		JwtChannelInterceptor interceptor = interceptor(new AtomicInteger());
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/topic/admin");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
				.isInstanceOf(MessageDeliveryException.class)
				.hasMessageContaining("Access denied to destination");
	}

	@Test
	void preSend_shouldRejectSubscribeToUserDestinationByDefault() {
		JwtChannelInterceptor interceptor = interceptor(new AtomicInteger());
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination("/user/queue/notifications");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
				.isInstanceOf(MessageDeliveryException.class)
				.hasMessageContaining("Access denied to destination");
	}

	@Test
	void preSend_shouldRejectClientSendToTopicDestination() {
		JwtChannelInterceptor interceptor = interceptor(new AtomicInteger());
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/topic/projects/" + UUID.randomUUID() + "/issues");

		assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
				.isInstanceOf(MessageDeliveryException.class);
	}

	@Test
	void preSend_shouldAllowClientSendToApplicationDestination() {
		JwtChannelInterceptor interceptor = interceptor(new AtomicInteger());
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/app/issues");
		Message<?> message = message(accessor);

		assertThat(interceptor.preSend(message, null)).isSameAs(message);
	}

	private JwtChannelInterceptor interceptor(AtomicInteger activeConnections) {
		return new JwtChannelInterceptor(
				jwtService,
				tokenBlacklist,
				userRepository,
				permissionEvaluator,
				projectRepository,
				activeConnections);
	}

	private Message<?> connectMessage(String token) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setNativeHeader("Authorization", "Bearer " + token);
		return message(accessor);
	}

	private Message<?> message(StompHeaderAccessor accessor) {
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}
}
