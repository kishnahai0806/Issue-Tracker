package com.krish.issuetracker.logging;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class MdcLoggingFilter extends OncePerRequestFilter {

	private static final String ANONYMOUS_USER = "anonymous";
	private static final String SPRING_ANONYMOUS_USER = "anonymousUser";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			MDC.put("requestId", UUID.randomUUID().toString());
			MDC.put("userId", currentUserId());
			MDC.put("path", request.getRequestURI());
			MDC.put("method", request.getMethod());
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}

	private String currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return ANONYMOUS_USER;
		}

		String userId = authentication.getName();
		if (userId == null || userId.isBlank() || SPRING_ANONYMOUS_USER.equals(userId)) {
			return ANONYMOUS_USER;
		}
		return userId;
	}
}
