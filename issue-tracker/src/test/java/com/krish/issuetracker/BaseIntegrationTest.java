package com.krish.issuetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
// Rolls back after each test method so integration tests do not pollute each other.
@Transactional
abstract class BaseIntegrationTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	@LocalServerPort
	protected int port;

	@Autowired
	protected ObjectMapper objectMapper;
}
