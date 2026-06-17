package com.krish.issuetracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketEventPublisher {

	private static final String ISSUE_UPDATES_CHANNEL = "ws:issue-updates";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public WebSocketEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	public void publishIssueUpdate(IssueUpdateEvent event) {
		try {
			String jsonPayload = objectMapper.writeValueAsString(event);
			redisTemplate.convertAndSend(ISSUE_UPDATES_CHANNEL, jsonPayload);
			log.debug("Published WebSocket issue event {} for issue {}", event.eventType(), event.issueId());
		} catch (Exception ex) {
			log.error("Failed to publish WebSocket issue update", ex);
		}
	}
}
