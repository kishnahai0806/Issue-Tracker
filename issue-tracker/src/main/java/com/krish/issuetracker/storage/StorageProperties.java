package com.krish.issuetracker.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "storage")
@Validated
public record StorageProperties(
		@NotBlank
		String endpoint,

		@NotBlank
		String accessKey,

		@NotBlank
		String secretKey,

		@NotBlank
		String bucketName,

		@DefaultValue("60")
		@Positive
		long presignedUrlExpiryMinutes,

		@DefaultValue("10485760")
		@Positive
		long maxFileSizeBytes) {
}
