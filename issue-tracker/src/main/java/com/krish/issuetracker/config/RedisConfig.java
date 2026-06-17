package com.krish.issuetracker.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

	private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
	private static final String ANALYTICS_TRENDS_CACHE = "analytics-trends";
	private static final String ANALYTICS_BURNDOWN_CACHE = "analytics-burndown";

	private final long analyticsTrendsTtlMinutes;
	private final long analyticsBurndownTtlMinutes;

	public RedisConfig(
			@Value("${cache.analytics.trends-ttl-minutes:60}") long analyticsTrendsTtlMinutes,
			@Value("${cache.analytics.burndown-ttl-minutes:60}") long analyticsBurndownTtlMinutes) {
		this.analyticsTrendsTtlMinutes = analyticsTrendsTtlMinutes;
		this.analyticsBurndownTtlMinutes = analyticsBurndownTtlMinutes;
	}

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(DEFAULT_TTL)
				.disableCachingNullValues()
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
						new GenericJackson2JsonRedisSerializer()));

		Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
				ANALYTICS_TRENDS_CACHE,
				defaultConfiguration.entryTtl(Duration.ofMinutes(analyticsTrendsTtlMinutes)),
				ANALYTICS_BURNDOWN_CACHE,
				defaultConfiguration.entryTtl(Duration.ofMinutes(analyticsBurndownTtlMinutes)));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfiguration)
				.withInitialCacheConfigurations(cacheConfigurations)
				.build();
	}
}
