package com.krish.issuetracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.krish.issuetracker.BaseIntegrationTest;
import com.krish.issuetracker.auth.dto.AuthResponse;
import com.krish.issuetracker.auth.dto.LoginRequest;
import com.krish.issuetracker.auth.dto.RefreshRequest;
import com.krish.issuetracker.auth.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends BaseIntegrationTest {

	@Test
	void register_shouldReturn201WithUserResponse() {
		String email = uniqueEmail();

		ResponseEntity<UserResponse> response = restTemplate.postForEntity(
				authUrl("/register"),
				registerRequest(email),
				UserResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().email()).isEqualTo(email);
		assertThat(response.getBody().id()).isNotNull();
	}

	@Test
	void register_shouldReturn409WhenEmailAlreadyExists() {
		String email = uniqueEmail();
		restTemplate.postForEntity(authUrl("/register"), registerRequest(email), UserResponse.class);

		ResponseEntity<String> response = restTemplate.postForEntity(
				authUrl("/register"),
				registerRequest(email),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void login_shouldReturn200WithTokens() {
		String email = uniqueEmail();
		restTemplate.postForEntity(authUrl("/register"), registerRequest(email), UserResponse.class);

		ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
				authUrl("/login"),
				new LoginRequest(email, PASSWORD),
				AuthResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().accessToken()).isNotBlank();
		assertThat(response.getBody().refreshToken()).isNotBlank();
		assertThat(response.getBody().tokenType()).isEqualTo("Bearer");
	}

	@Test
	void login_shouldReturn401WithWrongPassword() {
		String email = uniqueEmail();
		restTemplate.postForEntity(authUrl("/register"), registerRequest(email), UserResponse.class);

		ResponseEntity<String> response = restTemplate.postForEntity(
				authUrl("/login"),
				new LoginRequest(email, "wrong-password"),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void login_shouldReturn401WhenUserNotFound() {
		ResponseEntity<String> response = restTemplate.postForEntity(
				authUrl("/login"),
				new LoginRequest(uniqueEmail(), PASSWORD),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void refresh_shouldReturn200WithNewTokens() {
		AuthResponse loginResponse = registerAndLogin(uniqueEmail());

		ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
				authUrl("/refresh"),
				new RefreshRequest(loginResponse.refreshToken()),
				AuthResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().accessToken()).isNotBlank();
		assertThat(response.getBody().refreshToken()).isNotBlank();
	}

	@Test
	void logout_shouldReturn204() {
		AuthResponse loginResponse = registerAndLogin(uniqueEmail());

		ResponseEntity<Void> response = restTemplate.postForEntity(
				authUrl("/logout"),
				new HttpEntity<>(new RefreshRequest(loginResponse.refreshToken()), authHeaders(loginResponse.accessToken())),
				Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	private AuthResponse registerAndLogin(String email) {
		restTemplate.postForEntity(authUrl("/register"), registerRequest(email), UserResponse.class);
		ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
				authUrl("/login"),
				new LoginRequest(email, PASSWORD),
				AuthResponse.class);
		return response.getBody();
	}
}
