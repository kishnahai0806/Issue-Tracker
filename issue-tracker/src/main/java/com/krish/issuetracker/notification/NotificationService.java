package com.krish.issuetracker.notification;

import java.nio.charset.StandardCharsets;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@Slf4j
public class NotificationService {

	private final JavaMailSender javaMailSender;
	private final SpringTemplateEngine templateEngine;
	private final MailProperties mailProperties;
	private final MeterRegistry meterRegistry;

	public NotificationService(
			JavaMailSender javaMailSender,
			@Qualifier("emailTemplateEngine") SpringTemplateEngine templateEngine,
			MailProperties mailProperties,
			MeterRegistry meterRegistry,
			@Qualifier("emailTaskExecutor") ThreadPoolTaskExecutor emailTaskExecutor) {
		this.javaMailSender = javaMailSender;
		this.templateEngine = templateEngine;
		this.mailProperties = mailProperties;
		this.meterRegistry = meterRegistry;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Async("emailTaskExecutor")
	public void handleEmailNotification(EmailNotificationEvent event) {
		try {
			Context context = new Context();
			context.setVariables(event.getTemplateVariables());
			String body = templateEngine.process(event.getTemplateName(), context);

			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
			helper.setFrom(mailProperties.fromAddress(), mailProperties.fromName());
			helper.setTo(event.getRecipientEmail());
			helper.setSubject(event.getSubject());
			helper.setText(body, true);

			javaMailSender.send(mimeMessage);
			meterRegistry.counter("emails.sent").increment();
			log.info(
					"Email sent: template={}, recipient={}",
					event.getTemplateName(),
					maskEmail(event.getRecipientEmail()));
		} catch (Exception ex) {
			log.error(
					"Failed to send email notification: template={}, recipient={}",
					event.getTemplateName(),
					maskEmail(event.getRecipientEmail()),
					ex);
		}
	}

	private String maskEmail(String email) {
		if (email == null || email.isBlank()) {
			return "unknown";
		}
		int atIndex = email.indexOf('@');
		if (atIndex <= 0 || atIndex == email.length() - 1) {
			return "**redacted**";
		}
		String localPart = email.substring(0, atIndex);
		String domain = email.substring(atIndex + 1);
		String visible = localPart.length() <= 2 ? localPart : localPart.substring(0, 2);
		return visible + "***@" + domain;
	}
}
