package com.krish.issuetracker.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "mail")
@Validated
public record MailProperties(
		@NotBlank
		String host,

		@Positive
		int port,

		@NotBlank
		String username,

		@NotBlank
		String password,

		@NotBlank
		String fromAddress,

		@NotBlank
		String fromName,

		@DefaultValue("2")
		@Positive
		int threadPoolCoreSize,

		@DefaultValue("4")
		@Positive
		int threadPoolMaxSize,

		@DefaultValue("100")
		@Positive
		int threadPoolQueueCapacity) {
}
