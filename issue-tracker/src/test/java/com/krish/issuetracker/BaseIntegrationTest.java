package com.krish.issuetracker;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krish.issuetracker.auth.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
// Rolls back after each test method so integration tests do not pollute each other.
@Transactional
public abstract class BaseIntegrationTest {

	protected static final String PASSWORD = "Password1!";

	@Autowired
	protected TestRestTemplate restTemplate;

	@LocalServerPort
	protected int port;

	@Autowired
	protected ObjectMapper objectMapper;

	protected String uniqueEmail() {
		return "it_" + UUID.randomUUID() + "@test.com";
	}

	protected RegisterRequest registerRequest(String email) {
		return new RegisterRequest(email, PASSWORD, "Test User");
	}

	protected String authUrl(String path) {
		return "http://localhost:" + port + "/api/v1/auth" + path;
	}

	protected HttpHeaders authHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}
}
