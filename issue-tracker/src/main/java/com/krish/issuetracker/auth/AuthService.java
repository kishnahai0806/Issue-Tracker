package com.krish.issuetracker.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.auth.dto.AuthResponse;
import com.krish.issuetracker.auth.dto.LoginRequest;
import com.krish.issuetracker.auth.dto.RefreshRequest;
import com.krish.issuetracker.auth.dto.RegisterRequest;
import com.krish.issuetracker.auth.dto.UserResponse;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.TokenBlacklist;
import com.krish.issuetracker.security.jwt.JwtProperties;
import com.krish.issuetracker.security.jwt.JwtService;
import com.krish.issuetracker.security.session.RefreshTokenStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuthService {

	private static final String TOKEN_TYPE = "Bearer";
	private static final long MILLIS_PER_SECOND = 1_000L;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenStore refreshTokenStore;
	private final TokenBlacklist tokenBlacklist;
	private final JwtProperties jwtProperties;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenStore refreshTokenStore,
			TokenBlacklist tokenBlacklist,
			JwtProperties jwtProperties) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenStore = refreshTokenStore;
		this.tokenBlacklist = tokenBlacklist;
		this.jwtProperties = jwtProperties;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyExistsException(request.email());
		}

		User user = new User();
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFullName(request.fullName());
		user.setActive(true);
		user.setEmailVerified(false);

		User savedUser = userRepository.save(user);
		log.info("User registered: {}", savedUser.getId());

		return toUserResponse(savedUser);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}
		if (!user.isActive()) {
			throw new InvalidCredentialsException();
		}

		user.setLastLoginAt(LocalDateTime.now());
		userRepository.save(user);

		AuthResponse response = issueTokenPair(user);
		log.info("Login successful: {}", user.getId());

		return response;
	}

	@Transactional
	public AuthResponse refresh(RefreshRequest request) {
		String currentRefreshTokenHash = refreshTokenStore.hashToken(request.refreshToken());
		UUID userId = refreshTokenStore.consumeToken(currentRefreshTokenHash)
				.orElseThrow(InvalidRefreshTokenException::new);

		User user = userRepository.findById(userId)
				.orElseThrow(InvalidRefreshTokenException::new);

		if (!user.isActive()) {
			throw new InvalidRefreshTokenException();
		}

		AuthResponse response = issueTokenPair(user);
		log.info("Token refreshed: {}", user.getId());

		return response;
	}

	@Transactional
	public void logout(String rawAccessToken, String rawRefreshToken) {
		String refreshTokenHash = refreshTokenStore.hashToken(rawRefreshToken);
		Optional<UUID> userId = refreshTokenStore.findUserIdByTokenHash(refreshTokenHash);

		refreshTokenStore.revokeToken(refreshTokenHash);
		tokenBlacklist.blacklist(rawAccessToken, jwtProperties.getAccessTokenExpiryMs());
		userId.ifPresent(id -> log.info("Logout: {}", id));
	}

	private AuthResponse issueTokenPair(User user) {
		String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
		String rawRefreshToken = refreshTokenStore.generateRawToken();
		String refreshTokenHash = refreshTokenStore.hashToken(rawRefreshToken);

		refreshTokenStore.storeSession(
				refreshTokenHash,
				user.getId(),
				Duration.ofMillis(jwtProperties.getRefreshTokenExpiryMs()));

		return new AuthResponse(
				accessToken,
				rawRefreshToken,
				TOKEN_TYPE,
				jwtProperties.getAccessTokenExpiryMs() / MILLIS_PER_SECOND);
	}

	private UserResponse toUserResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.isActive(),
				user.isEmailVerified(),
				user.getCreatedAt());
	}
}
