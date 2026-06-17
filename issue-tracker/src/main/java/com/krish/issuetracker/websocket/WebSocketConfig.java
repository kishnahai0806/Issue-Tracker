package com.krish.issuetracker.websocket;

import java.util.Arrays;
import java.util.List;

import com.krish.issuetracker.config.InvalidCorsConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final String allowedOrigins;

	public WebSocketConfig(@Value("${cors.allowed-origins}") String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		List<String> origins = allowedOrigins();
		registry.addEndpoint("/ws")
				.setAllowedOrigins(origins.toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic", "/user");
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	private List<String> allowedOrigins() {
		List<String> origins = Arrays.stream(StringUtils.commaDelimitedListToStringArray(allowedOrigins))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();

		if (origins.isEmpty() || origins.contains("*")) {
			throw new InvalidCorsConfigurationException("Wildcard or empty WebSocket origins are not allowed");
		}

		return origins;
	}
}
