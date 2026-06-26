package com.krish.issuetracker.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Test
	void consumeToken_shouldReturnUserIdFromAtomicScript() {
		UUID userId = UUID.randomUUID();
		RefreshTokenStore store = new RefreshTokenStore(redisTemplate);
		when(redisTemplate.execute(
				any(RedisScript.class),
				eq(List.of("refresh:token-hash")),
				eq("token-hash"),
				eq("refresh:user:")))
				.thenReturn(userId.toString());

		Optional<UUID> result = store.consumeToken("token-hash");

		assertThat(result).contains(userId);
	}

	@Test
	void consumeToken_shouldReturnEmptyWhenTokenMissing() {
		RefreshTokenStore store = new RefreshTokenStore(redisTemplate);
		when(redisTemplate.execute(
				any(RedisScript.class),
				eq(List.of("refresh:token-hash")),
				eq("token-hash"),
				eq("refresh:user:")))
				.thenReturn(null);

		Optional<UUID> result = store.consumeToken("token-hash");

		assertThat(result).isEmpty();
	}
}
