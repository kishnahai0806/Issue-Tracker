package com.krish.issuetracker.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "test-secret-key-minimum-32-characters-long";
	private static final String ISSUER = "issue-tracker-test";
	private static final String AUDIENCE = "issue-tracker-client-test";

	private JwtService jwtServiceWithAccessExpiry(long accessTokenExpiryMs) {
		JwtProperties properties = new JwtProperties(SECRET, accessTokenExpiryMs, 604800000L, ISSUER, AUDIENCE);
		return new JwtService(properties);
	}

	@Test
	void generateAccessToken_shouldProduceTokenWithUserIdAndEmail() {
		JwtService jwtService = jwtServiceWithAccessExpiry(900000L);
		UUID userId = UUID.randomUUID();

		String token = jwtService.generateAccessToken(userId, "user@test.com");

		assertThat(jwtService.validateToken(token)).isTrue();
		assertThat(jwtService.extractUserId(token)).isEqualTo(userId.toString());
		assertThat(jwtService.extractEmail(token)).isEqualTo("user@test.com");
	}

	@Test
	void validateToken_shouldReturnFalseForMalformedToken() {
		JwtService jwtService = jwtServiceWithAccessExpiry(900000L);

		assertThat(jwtService.validateToken("not-a-jwt")).isFalse();
	}

	@Test
	void isExpired_shouldReturnFalseForValidToken() {
		JwtService jwtService = jwtServiceWithAccessExpiry(900000L);
		String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@test.com");

		assertThat(jwtService.isExpired(token)).isFalse();
	}

	@Test
	void isExpired_shouldReturnTrueForExpiredToken() {
		JwtService jwtService = jwtServiceWithAccessExpiry(-1000L);
		String expiredToken = jwtService.generateAccessToken(UUID.randomUUID(), "user@test.com");

		assertThat(jwtService.validateToken(expiredToken)).isFalse();
		assertThat(jwtService.isExpired(expiredToken)).isTrue();
	}

	@Test
	void isExpired_shouldReturnFalseForMalformedToken() {
		JwtService jwtService = jwtServiceWithAccessExpiry(900000L);

		assertThat(jwtService.isExpired("not-a-jwt")).isFalse();
	}
}
