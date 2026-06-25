package com.krish.issuetracker.security.ratelimit;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

	/*
	 * A dedicated Lettuce client backs Bucket4j so rate-limit state lives in Redis and is shared
	 * across every application replica. Connection details are read from RedisConnectionDetails so
	 * the same wiring resolves Spring properties in production and the Testcontainers Redis in tests.
	 */
	@Bean(destroyMethod = "shutdown")
	public RedisClient rateLimitRedisClient(RedisConnectionDetails redisConnectionDetails) {
		RedisConnectionDetails.Standalone standalone = redisConnectionDetails.getStandalone();
		if (standalone == null) {
			throw new IllegalStateException("Rate limiting requires a standalone Redis connection");
		}

		RedisURI.Builder uriBuilder = RedisURI.builder()
				.withHost(standalone.getHost())
				.withPort(standalone.getPort())
				.withDatabase(standalone.getDatabase());

		String username = redisConnectionDetails.getUsername();
		String password = redisConnectionDetails.getPassword();
		if (StringUtils.hasText(password)) {
			if (StringUtils.hasText(username)) {
				uriBuilder.withAuthentication(username, password.toCharArray());
			} else {
				uriBuilder.withPassword(password.toCharArray());
			}
		}

		return RedisClient.create(uriBuilder.build());
	}

	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient rateLimitRedisClient) {
		return rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
	}

	@Bean
	public ProxyManager<String> rateLimitProxyManager(
			StatefulRedisConnection<String, byte[]> rateLimitRedisConnection,
			RateLimitProperties rateLimitProperties) {
		// Keys expire once their bucket would be fully refilled, so abandoned limiters never persist.
		return LettuceBasedProxyManager.builderFor(rateLimitRedisConnection)
				.withExpirationStrategy(ExpirationAfterWriteStrategy
						.basedOnTimeForRefillingBucketUpToMax(maxRefillPeriod(rateLimitProperties)))
				.build();
	}

	@Bean
	public AuthRateLimitFilter authRateLimitFilter(
			ProxyManager<String> rateLimitProxyManager,
			RateLimitProperties rateLimitProperties,
			MeterRegistry meterRegistry,
			ObjectMapper objectMapper) {
		return new AuthRateLimitFilter(rateLimitProxyManager, rateLimitProperties, meterRegistry, objectMapper);
	}

	/*
	 * Disabled registration keeps the filter out of the default servlet chain; it runs only where
	 * SecurityConfig places it, mirroring how JwtAuthenticationFilter is wired.
	 */
	@Bean
	public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(
			AuthRateLimitFilter authRateLimitFilter) {
		FilterRegistrationBean<AuthRateLimitFilter> registrationBean = new FilterRegistrationBean<>(authRateLimitFilter);
		registrationBean.setEnabled(false);
		return registrationBean;
	}

	private Duration maxRefillPeriod(RateLimitProperties properties) {
		Duration max = properties.login().refillPeriod();
		if (properties.register().refillPeriod().compareTo(max) > 0) {
			max = properties.register().refillPeriod();
		}
		if (properties.refresh().refillPeriod().compareTo(max) > 0) {
			max = properties.refresh().refillPeriod();
		}
		return max;
	}
}
