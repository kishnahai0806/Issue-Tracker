package com.krish.issuetracker.auth;

import com.krish.issuetracker.auth.dto.AuthResponse;
import com.krish.issuetracker.auth.dto.LoginRequest;
import com.krish.issuetracker.auth.dto.RefreshRequest;
import com.krish.issuetracker.auth.dto.RegisterRequest;
import com.krish.issuetracker.auth.dto.UserResponse;
import com.krish.issuetracker.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse response = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
		return ResponseEntity.ok(authService.refresh(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
			@Valid @RequestBody RefreshRequest request,
			HttpServletRequest servletRequest) {
		String accessToken = extractBearerToken(servletRequest);
		requireAuthenticatedAccessToken(accessToken);

		authService.logout(accessToken, request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	private String extractBearerToken(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			throw new InvalidCredentialsException();
		}

		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	private void requireAuthenticatedAccessToken(String accessToken) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| authentication instanceof AnonymousAuthenticationToken
				|| !authentication.isAuthenticated()
				|| !jwtService.validateToken(accessToken)) {
			throw new InvalidCredentialsException();
		}
	}
}
