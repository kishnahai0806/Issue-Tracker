package com.krish.issuetracker.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	void blacklist_shouldUseHashedTokenKey() throws Exception {
		String token = "header.payload.signature";
		RedisTokenBlacklist blacklist = new RedisTokenBlacklist(redisTemplate);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		blacklist.blacklist(token, 900000L);

		String expectedKey = "blacklist:" + sha256Base64Url(token);
		verify(valueOperations).set(eq(expectedKey), eq("1"), eq(Duration.ofMillis(900000L)));
		assertThat(expectedKey).doesNotContain(token);
	}

	@Test
	void isBlacklisted_shouldUseHashedTokenKey() throws Exception {
		String token = "header.payload.signature";
		RedisTokenBlacklist blacklist = new RedisTokenBlacklist(redisTemplate);
		String expectedKey = "blacklist:" + sha256Base64Url(token);
		when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

		assertThat(blacklist.isBlacklisted(token)).isTrue();

		verify(redisTemplate).hasKey(expectedKey);
	}

	private String sha256Base64Url(String value) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}
}
