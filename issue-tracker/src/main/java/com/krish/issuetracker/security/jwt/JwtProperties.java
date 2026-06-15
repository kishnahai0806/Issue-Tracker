package com.krish.issuetracker.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	@NotBlank
	private final String secret;

	@Positive
	private final long accessTokenExpiryMs;

	@Positive
	private final long refreshTokenExpiryMs;

	@NotBlank
	private final String issuer;

	@NotBlank
	private final String audience;

	public JwtProperties(
			String secret,
			long accessTokenExpiryMs,
			long refreshTokenExpiryMs,
			String issuer,
			String audience) {
		this.secret = secret;
		this.accessTokenExpiryMs = accessTokenExpiryMs;
		this.refreshTokenExpiryMs = refreshTokenExpiryMs;
		this.issuer = issuer;
		this.audience = audience;
	}

	public String getSecret() {
		return secret;
	}

	public long getAccessTokenExpiryMs() {
		return accessTokenExpiryMs;
	}

	public long getRefreshTokenExpiryMs() {
		return refreshTokenExpiryMs;
	}

	public String getIssuer() {
		return issuer;
	}

	public String getAudience() {
		return audience;
	}
}
