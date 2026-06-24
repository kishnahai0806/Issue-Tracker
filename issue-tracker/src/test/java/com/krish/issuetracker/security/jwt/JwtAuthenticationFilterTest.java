package com.krish.issuetracker.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.TokenBlacklist;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private TokenBlacklist tokenBlacklist;

	@Mock
	private UserRepository userRepository;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
		MDC.clear();
	}

	@Test
	void doFilterInternal_shouldAuthenticateActiveUser() throws Exception {
		String token = "access-token";
		UUID userId = UUID.randomUUID();
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
				jwtService,
				tokenBlacklist,
				userRepository,
				meterRegistry);
		MockHttpServletRequest request = requestWithBearerToken(token);
		User user = new User();
		user.setId(userId);
		user.setActive(true);
		when(jwtService.validateToken(token)).thenReturn(true);
		when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
		when(jwtService.extractUserId(token)).thenReturn(userId.toString());
		when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.of(user));

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(userId.toString());
		assertThat(MDC.get("userId")).isEqualTo(userId.toString());
	}

	@Test
	void doFilterInternal_shouldSkipAuthenticationWhenUserIsDisabled() throws Exception {
		String token = "access-token";
		UUID userId = UUID.randomUUID();
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
				jwtService,
				tokenBlacklist,
				userRepository,
				meterRegistry);
		MockHttpServletRequest request = requestWithBearerToken(token);
		when(jwtService.validateToken(token)).thenReturn(true);
		when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);
		when(jwtService.extractUserId(token)).thenReturn(userId.toString());
		when(userRepository.findByIdAndIsActiveTrue(userId)).thenReturn(Optional.empty());

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
		assertThat(meterRegistry.counter("auth.failures", "reason", "ACCOUNT_DISABLED").count()).isEqualTo(1.0);
	}

	private MockHttpServletRequest requestWithBearerToken(String token) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		return request;
	}
}
