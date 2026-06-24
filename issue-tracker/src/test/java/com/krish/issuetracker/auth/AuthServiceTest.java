package com.krish.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.auth.dto.AuthResponse;
import com.krish.issuetracker.auth.dto.LoginRequest;
import com.krish.issuetracker.auth.dto.RefreshRequest;
import com.krish.issuetracker.auth.dto.RegisterRequest;
import com.krish.issuetracker.auth.dto.UserResponse;
import com.krish.issuetracker.domain.entity.User;
import com.krish.issuetracker.repository.OrganizationMemberRepository;
import com.krish.issuetracker.repository.UserRepository;
import com.krish.issuetracker.security.TokenBlacklist;
import com.krish.issuetracker.security.jwt.JwtProperties;
import com.krish.issuetracker.security.jwt.JwtService;
import com.krish.issuetracker.security.session.RefreshTokenStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String EMAIL = "test@example.com";
	private static final String PASSWORD = "password123";
	private static final String FULL_NAME = "Test User";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Mock
	private JwtProperties jwtProperties;

	@Mock
	private OrganizationMemberRepository organizationMemberRepository;

	@Mock
	private TokenBlacklist tokenBlacklist;

	@InjectMocks
	private AuthService authService;

	@Test
	void register_shouldSaveUserAndReturnResponse() {
		UUID userId = UUID.randomUUID();
		when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(passwordEncoder.encode(PASSWORD)).thenReturn("hashedPassword");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(userId);
			return user;
		});

		UserResponse response = authService.register(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME));

		assertThat(response).isNotNull();
		assertThat(response.email()).isEqualTo(EMAIL);
		verify(userRepository).save(any(User.class));
		verify(passwordEncoder).encode(PASSWORD);
	}

	@Test
	void register_shouldThrowWhenEmailAlreadyExists() {
		when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, PASSWORD, FULL_NAME)))
				.isInstanceOf(EmailAlreadyExistsException.class);
	}

	@Test
	void login_shouldReturnTokensOnValidCredentials() {
		User user = activeUser();
		user.setPasswordHash("hash");
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);
		when(jwtService.generateAccessToken(user.getId(), EMAIL)).thenReturn("access-token");
		when(refreshTokenStore.generateRawToken()).thenReturn("raw-refresh");
		when(refreshTokenStore.hashToken("raw-refresh")).thenReturn("hashed-refresh");
		when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(604800000L);
		when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(900000L);

		AuthResponse response = authService.login(new LoginRequest(EMAIL, PASSWORD));

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("raw-refresh");
		verify(refreshTokenStore).storeSession(any(), any(), any());
	}

	@Test
	void login_shouldThrowInvalidCredentialsWhenUserNotFound() {
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void login_shouldThrowInvalidCredentialsWhenPasswordWrong() {
		User user = activeUser();
		user.setPasswordHash("hash");
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void login_shouldThrowInvalidCredentialsWhenUserDisabled() {
		User user = activeUser();
		user.setActive(false);
		user.setPasswordHash("hash");
		when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches(PASSWORD, "hash")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void refresh_shouldCheckUserIsActiveBeforeIssuingTokens() {
		UUID userId = UUID.randomUUID();
		User user = activeUser();
		user.setId(userId);
		user.setActive(false);
		when(refreshTokenStore.hashToken("raw-refresh")).thenReturn("hash");
		when(refreshTokenStore.consumeToken("hash")).thenReturn(Optional.of(userId));
		when(userRepository.findById(userId)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.refresh(new RefreshRequest("raw-refresh")))
				.isInstanceOf(InvalidRefreshTokenException.class);
	}

	private User activeUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(EMAIL);
		user.setFullName(FULL_NAME);
		user.setActive(true);
		user.setEmailVerified(false);
		return user;
	}
}
