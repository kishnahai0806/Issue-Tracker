package com.krish.issuetracker.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisWebSocketConfig {

	private static final String ISSUE_UPDATES_CHANNEL = "ws:issue-updates";

	private final RedisConnectionFactory redisConnectionFactory;
	private final WebSocketEventSubscriber subscriber;

	public RedisWebSocketConfig(
			RedisConnectionFactory redisConnectionFactory,
			WebSocketEventSubscriber subscriber) {
		this.redisConnectionFactory = redisConnectionFactory;
		this.subscriber = subscriber;
	}

	@Bean
	public RedisMessageListenerContainer redisMessageListenerContainer() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener(subscriber, new ChannelTopic(ISSUE_UPDATES_CHANNEL));
		return container;
	}
}
