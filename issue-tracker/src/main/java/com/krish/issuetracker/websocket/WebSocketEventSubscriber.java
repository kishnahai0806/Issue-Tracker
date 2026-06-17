package com.krish.issuetracker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketEventSubscriber implements MessageListener {

	private static final String PROJECT_TOPIC_PREFIX = "/topic/projects/";
	private static final String PROJECT_TOPIC_SUFFIX = "/issues";

	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;

	public WebSocketEventSubscriber(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			IssueUpdateEvent event = objectMapper.readValue(message.getBody(), IssueUpdateEvent.class);
			String destination = PROJECT_TOPIC_PREFIX + event.projectId() + PROJECT_TOPIC_SUFFIX;
			messagingTemplate.convertAndSend(destination, event);
			log.debug("Received and forwarded WebSocket issue event {} for issue {}", event.eventType(), event.issueId());
		} catch (Exception ex) {
			log.error("Failed to forward WebSocket issue update", ex);
		}
	}
}
