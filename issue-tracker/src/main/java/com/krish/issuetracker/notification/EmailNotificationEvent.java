package com.krish.issuetracker.notification;

import java.util.Map;

import org.springframework.context.ApplicationEvent;

public class EmailNotificationEvent extends ApplicationEvent {

	private final String recipientEmail;
	private final String recipientName;
	private final String subject;
	private final String templateName;
	private final Map<String, Object> templateVariables;

	public EmailNotificationEvent(
			Object source,
			String recipientEmail,
			String recipientName,
			String subject,
			String templateName,
			Map<String, Object> templateVariables) {
		super(source);
		this.recipientEmail = recipientEmail;
		this.recipientName = recipientName;
		this.subject = subject;
		this.templateName = templateName;
		this.templateVariables = templateVariables == null ? Map.of() : Map.copyOf(templateVariables);
	}

	public String getRecipientEmail() {
		return recipientEmail;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getSubject() {
		return subject;
	}

	public String getTemplateName() {
		return templateName;
	}

	public Map<String, Object> getTemplateVariables() {
		return templateVariables;
	}
}
