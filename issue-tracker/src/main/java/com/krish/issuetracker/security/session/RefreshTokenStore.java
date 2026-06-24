package com.krish.issuetracker.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenStore {

	private static final int REFRESH_TOKEN_BYTES = 32;
	private static final String HASH_ALGORITHM = "SHA-256";
	private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
	private static final String USER_REFRESH_TOKENS_KEY_PREFIX = "refresh:user:";
	private static final RedisScript<String> CONSUME_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
			local userId = redis.call('GET', KEYS[1])
			if not userId then
				return nil
			end
			redis.call('DEL', KEYS[1])
			redis.call('SREM', ARGV[2] .. userId, ARGV[1])
			return userId
			""", String.class);

	private final StringRedisTemplate redisTemplate;
	private final SecureRandom secureRandom;

	public RefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.secureRandom = new SecureRandom();
	}

	public String generateRawToken() {
		byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	public String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new TokenHashingException(HASH_ALGORITHM + " is not available", ex);
		}
	}

	public void storeSession(String tokenHash, UUID userId, Duration ttl) {
		String refreshKey = refreshKey(tokenHash);
		String userRefreshTokensKey = userRefreshTokensKey(userId);

		redisTemplate.opsForValue().set(refreshKey, userId.toString(), ttl);
		redisTemplate.opsForSet().add(userRefreshTokensKey, tokenHash);
		redisTemplate.expire(userRefreshTokensKey, ttl);
	}

	public Optional<UUID> findUserIdByTokenHash(String tokenHash) {
		String userId = redisTemplate.opsForValue().get(refreshKey(tokenHash));
		if (userId == null) {
			return Optional.empty();
		}
		return Optional.of(UUID.fromString(userId));
	}

	public Optional<UUID> consumeToken(String tokenHash) {
		String userId = redisTemplate.execute(
				CONSUME_REFRESH_TOKEN_SCRIPT,
				List.of(refreshKey(tokenHash)),
				tokenHash,
				USER_REFRESH_TOKENS_KEY_PREFIX);
		if (userId == null) {
			return Optional.empty();
		}
		return Optional.of(UUID.fromString(userId));
	}

	public void revokeToken(String tokenHash) {
		String refreshKey = refreshKey(tokenHash);
		String userId = redisTemplate.opsForValue().get(refreshKey);

		redisTemplate.delete(refreshKey);

		if (userId != null) {
			redisTemplate.opsForSet().remove(userRefreshTokensKey(userId), tokenHash);
		}
	}

	public void revokeAllTokensForUser(UUID userId) {
		String userRefreshTokensKey = userRefreshTokensKey(userId);
		Set<String> tokenHashes = redisTemplate.opsForSet().members(userRefreshTokensKey);

		if (tokenHashes != null && !tokenHashes.isEmpty()) {
			List<String> refreshKeys = tokenHashes.stream()
					.map(this::refreshKey)
					.toList();
			redisTemplate.delete(refreshKeys);
		}

		redisTemplate.delete(userRefreshTokensKey);
	}

	private String refreshKey(String tokenHash) {
		return REFRESH_TOKEN_KEY_PREFIX + tokenHash;
	}

	private String userRefreshTokensKey(UUID userId) {
		return userRefreshTokensKey(userId.toString());
	}

	private String userRefreshTokensKey(String userId) {
		return USER_REFRESH_TOKENS_KEY_PREFIX + userId;
	}
}
