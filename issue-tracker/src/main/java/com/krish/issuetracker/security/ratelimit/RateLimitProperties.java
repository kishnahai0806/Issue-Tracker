package com.krish.issuetracker.security.ratelimit;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
		@DefaultValue("true")
		boolean enabled,

		@Valid
		@NotNull
		@DefaultValue
		EndpointLimit login,

		@Valid
		@NotNull
		@DefaultValue
		EndpointLimit register,

		@Valid
		@NotNull
		@DefaultValue
		EndpointLimit refresh) {

	public record EndpointLimit(
			@Positive
			@DefaultValue("5")
			long capacity,

			@NotNull
			@DefaultValue("1m")
			Duration refillPeriod) {
	}
}
