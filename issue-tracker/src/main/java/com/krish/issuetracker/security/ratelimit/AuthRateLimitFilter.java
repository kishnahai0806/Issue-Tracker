package com.krish.issuetracker.security.ratelimit;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krish.issuetracker.exception.GlobalExceptionHandler.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/*
 * Throttles unauthenticated auth endpoints per client IP using Redis-backed Bucket4j buckets, so
 * brute-force attempts are limited consistently across replicas while legitimate users recover as
 * the token bucket refills. Exceeded requests receive 429 with a Retry-After hint.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final String LOGIN_PATH = "/api/v1/auth/login";
	private static final String REGISTER_PATH = "/api/v1/auth/register";
	private static final String REFRESH_PATH = "/api/v1/auth/refresh";
	private static final String KEY_PREFIX = "rate-limit:";
	private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
	private static final String REMAINING_HEADER = "X-Rate-Limit-Remaining";
	private static final String RATE_LIMIT_METRIC = "auth.rate_limit.exceeded";
	private static final String ENDPOINT_TAG = "endpoint";

	private final ProxyManager<String> proxyManager;
	private final RateLimitProperties properties;
	private final MeterRegistry meterRegistry;
	private final ObjectMapper objectMapper;

	public AuthRateLimitFilter(
			ProxyManager<String> proxyManager,
			RateLimitProperties properties,
			MeterRegistry meterRegistry,
			ObjectMapper objectMapper) {
		this.proxyManager = proxyManager;
		this.properties = properties;
		this.meterRegistry = meterRegistry;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !properties.enabled()
				|| !HttpMethod.POST.matches(request.getMethod())
				|| resolveEndpoint(request) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		EndpointRateLimit endpoint = resolveEndpoint(request);
		String key = KEY_PREFIX + endpoint.name() + ":" + clientIp(request);
		Bucket bucket = proxyManager.builder().build(key, configurationSupplier(endpoint.limit()));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			response.setHeader(REMAINING_HEADER, Long.toString(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
			return;
		}

		rejectRequest(request, response, endpoint, probe);
	}

	private EndpointRateLimit resolveEndpoint(HttpServletRequest request) {
		String path = request.getRequestURI();
		if (LOGIN_PATH.equals(path)) {
			return new EndpointRateLimit("login", properties.login());
		}
		if (REGISTER_PATH.equals(path)) {
			return new EndpointRateLimit("register", properties.register());
		}
		if (REFRESH_PATH.equals(path)) {
			return new EndpointRateLimit("refresh", properties.refresh());
		}
		return null;
	}

	private Supplier<BucketConfiguration> configurationSupplier(RateLimitProperties.EndpointLimit limit) {
		Bandwidth bandwidth = Bandwidth.builder()
				.capacity(limit.capacity())
				.refillGreedy(limit.capacity(), limit.refillPeriod())
				.build();
		BucketConfiguration configuration = BucketConfiguration.builder()
				.addLimit(bandwidth)
				.build();
		return () -> configuration;
	}

	private String clientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
		if (StringUtils.hasText(forwardedFor)) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private void rejectRequest(
			HttpServletRequest request,
			HttpServletResponse response,
			EndpointRateLimit endpoint,
			ConsumptionProbe probe) throws IOException {
		meterRegistry.counter(RATE_LIMIT_METRIC, ENDPOINT_TAG, endpoint.name()).increment();

		long retryAfterSeconds = Math.max(1L, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));

		ErrorResponse body = new ErrorResponse(
				Instant.now(),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				"Too many requests, please retry later",
				request.getRequestURI());
		objectMapper.writeValue(response.getWriter(), body);
	}

	private record EndpointRateLimit(String name, RateLimitProperties.EndpointLimit limit) {
	}
}
