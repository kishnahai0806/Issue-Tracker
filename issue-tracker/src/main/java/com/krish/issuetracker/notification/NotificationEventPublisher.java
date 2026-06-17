package com.krish.issuetracker.notification;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public NotificationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void publishEmailNotification(
			String recipientEmail,
			String recipientName,
			String subject,
			String templateName,
			Map<String, Object> templateVariables) {
		EmailNotificationEvent event = new EmailNotificationEvent(
				this,
				recipientEmail,
				recipientName,
				subject,
				templateName,
				templateVariables);
		applicationEventPublisher.publishEvent(event);
		log.debug("Published email notification event for template {}", templateName);
	}
}
