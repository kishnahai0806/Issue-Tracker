package com.krish.issuetracker.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;

import com.krish.issuetracker.security.TokenBlacklist;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTokenBlacklist implements TokenBlacklist {

	private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
	private static final String BLACKLIST_VALUE = "1";
	private static final String HASH_ALGORITHM = "SHA-256";

	private final StringRedisTemplate redisTemplate;

	public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void blacklist(String token, long expiryMs) {
		if (expiryMs <= 0) {
			return;
		}

		redisTemplate.opsForValue().set(blacklistKey(token), BLACKLIST_VALUE, Duration.ofMillis(expiryMs));
	}

	@Override
	public boolean isBlacklisted(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(token)));
	}

	private String blacklistKey(String token) {
		return BLACKLIST_KEY_PREFIX + hashToken(token);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new TokenHashingException(HASH_ALGORITHM + " is not available", ex);
		}
	}
}
