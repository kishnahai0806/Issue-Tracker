package com.krish.issuetracker.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtService {

	private static final String EMAIL_CLAIM = "email";

	private final JwtProperties jwtProperties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(UUID userId, String email) {
		Date issuedAt = new Date();
		Date expiresAt = new Date(issuedAt.getTime() + jwtProperties.getAccessTokenExpiryMs());

		return Jwts.builder()
				.subject(userId.toString())
				.claim(EMAIL_CLAIM, email)
				.issuer(jwtProperties.getIssuer())
				.audience()
				.add(jwtProperties.getAudience())
				.and()
				.issuedAt(issuedAt)
				.expiration(expiresAt)
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();
	}

	public String extractUserId(String token) {
		return parseClaims(token).getSubject();
	}

	public String extractEmail(String token) {
		return parseClaims(token).get(EMAIL_CLAIM, String.class);
	}

	public boolean validateToken(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	public boolean isExpired(String token) {
		try {
			parseClaims(token);
			return false;
		} catch (ExpiredJwtException ex) {
			return true;
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.requireIssuer(jwtProperties.getIssuer())
				.requireAudience(jwtProperties.getAudience())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
