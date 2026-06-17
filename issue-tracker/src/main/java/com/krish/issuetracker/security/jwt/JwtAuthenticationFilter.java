package com.krish.issuetracker.security.jwt;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.krish.issuetracker.security.TokenBlacklist;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final TokenBlacklist tokenBlacklist;
	private final MeterRegistry meterRegistry;

	public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklist tokenBlacklist, MeterRegistry meterRegistry) {
		this.jwtService = jwtService;
		this.tokenBlacklist = tokenBlacklist;
		this.meterRegistry = meterRegistry;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			extractToken(request).ifPresent(token -> authenticateToken(token, request));
		} catch (Exception ex) {
			log.debug("JWT authentication skipped", ex);
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}

	private void authenticateToken(String token, HttpServletRequest request) {
		if (!jwtService.validateToken(token)) {
			meterRegistry.counter("auth.failures", "reason", "TOKEN_INVALID").increment();
			SecurityContextHolder.clearContext();
			return;
		}
		if (tokenBlacklist.isBlacklisted(token)) {
			meterRegistry.counter("auth.failures", "reason", "TOKEN_EXPIRED").increment();
			SecurityContextHolder.clearContext();
			return;
		}

		String userId = jwtService.extractUserId(token);
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(userId, null, List.of());
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);
		MDC.put("userId", userId);
	}

	private Optional<String> extractToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return Optional.empty();
		}

		return Optional.of(authorizationHeader.substring(BEARER_PREFIX.length()));
	}
}
