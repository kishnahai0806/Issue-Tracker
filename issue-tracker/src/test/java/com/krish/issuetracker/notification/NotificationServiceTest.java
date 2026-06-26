package com.krish.issuetracker.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private JavaMailSender javaMailSender;

	@Mock
	private SpringTemplateEngine templateEngine;

	@Mock
	private MailProperties mailProperties;

	@Mock
	private ThreadPoolTaskExecutor emailTaskExecutor;

	@Spy
	private MeterRegistry meterRegistry = new SimpleMeterRegistry();

	@InjectMocks
	private NotificationService notificationService;

	private ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	void attachLogAppender() {
		listAppender = new ListAppender<>();
		listAppender.start();
		logger().addAppender(listAppender);
	}

	@AfterEach
	void detachLogAppender() {
		logger().detachAppender(listAppender);
	}

	@Test
	void handleEmailNotification_shouldRenderTemplateAndSendEmail_whenSuccessful() {
		EmailNotificationEvent event = emailEvent("user@example.com", "issue-assigned");
		when(templateEngine.process(eq("issue-assigned"), any(Context.class))).thenReturn("<p>Hello</p>");
		when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
		when(mailProperties.fromAddress()).thenReturn("test@issuetracker.com");
		when(mailProperties.fromName()).thenReturn("Issue Tracker Test");

		notificationService.handleEmailNotification(event);

		verify(templateEngine).process(eq("issue-assigned"), any(Context.class));
		verify(javaMailSender).send(any(MimeMessage.class));
	}

	@Test
	void handleEmailNotification_shouldLogErrorAndNotThrow_whenTemplateRenderingFails() {
		EmailNotificationEvent event = emailEvent("user@example.com", "issue-assigned");
		when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("template error"));

		assertThatNoException().isThrownBy(() -> notificationService.handleEmailNotification(event));

		verify(javaMailSender, never()).send(any(MimeMessage.class));
		assertThat(listAppender.list).anyMatch(this::isErrorLevel);
	}

	@Test
	void handleEmailNotification_shouldLogErrorAndNotThrow_whenJavaMailSendFails() {
		EmailNotificationEvent event = emailEvent("user@example.com", "issue-assigned");
		when(templateEngine.process(eq("issue-assigned"), any(Context.class))).thenReturn("<p>Hello</p>");
		when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
		when(mailProperties.fromAddress()).thenReturn("test@issuetracker.com");
		when(mailProperties.fromName()).thenReturn("Issue Tracker Test");
		doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(MimeMessage.class));

		assertThatNoException().isThrownBy(() -> notificationService.handleEmailNotification(event));

		assertThat(listAppender.list).anyMatch(this::isErrorLevel);
	}

	@Test
	void handleEmailNotification_shouldMaskEmailInLogs_whenSendingSucceeds() {
		String fullEmail = "verysecret@example.com";
		EmailNotificationEvent event = emailEvent(fullEmail, "issue-assigned");
		when(templateEngine.process(eq("issue-assigned"), any(Context.class))).thenReturn("<p>Hello</p>");
		when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
		when(mailProperties.fromAddress()).thenReturn("test@issuetracker.com");
		when(mailProperties.fromName()).thenReturn("Issue Tracker Test");

		notificationService.handleEmailNotification(event);

		List<String> messages = listAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
		assertThat(messages).noneMatch(message -> message.contains(fullEmail));
		assertThat(messages).anyMatch(message -> message.contains("ve***@example.com"));
	}

	private boolean isErrorLevel(ILoggingEvent event) {
		return event.getLevel().toString().equals("ERROR");
	}

	private Logger logger() {
		return (Logger) LoggerFactory.getLogger(NotificationService.class);
	}

	private MimeMessage realMimeMessage() {
		return new MimeMessage(Session.getDefaultInstance(new Properties()));
	}

	private EmailNotificationEvent emailEvent(String recipientEmail, String templateName) {
		return new EmailNotificationEvent(
				this,
				recipientEmail,
				"Recipient Name",
				"Test Subject",
				templateName,
				Map.of("recipientName", "Recipient Name"));
	}
}
