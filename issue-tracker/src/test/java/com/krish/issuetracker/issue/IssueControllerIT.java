package com.krish.issuetracker.issue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.BaseIntegrationTest;
import com.krish.issuetracker.auth.dto.AuthResponse;
import com.krish.issuetracker.auth.dto.LoginRequest;
import com.krish.issuetracker.auth.dto.UserResponse;
import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.issue.dto.CreateIssueRequest;
import com.krish.issuetracker.issue.dto.IssueDetailResponse;
import com.krish.issuetracker.issue.dto.IssueResponse;
import com.krish.issuetracker.issue.dto.PagedIssueResponse;
import com.krish.issuetracker.issue.dto.UpdateIssueRequest;
import com.krish.issuetracker.organization.dto.CreateOrganizationRequest;
import com.krish.issuetracker.organization.dto.OrganizationResponse;
import com.krish.issuetracker.project.dto.CreateProjectRequest;
import com.krish.issuetracker.project.dto.ProjectResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IssueControllerIT extends BaseIntegrationTest {

	@Test
	void createIssue_shouldReturn201() {
		TestSetup setup = setupOrgAndProject();
		CreateIssueRequest request = createIssueRequest(setup.projectId(), "Create issue test", IssueStatus.TODO);

		ResponseEntity<IssueResponse> response = restTemplate.postForEntity(
				issueUrl(setup.orgId(), setup.projectId(), ""),
				new HttpEntity<>(request, authHeaders(setup.accessToken())),
				IssueResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().issueNumber()).isGreaterThanOrEqualTo(1);
		assertThat(response.getBody().title()).isEqualTo(request.title());
	}

	@Test
	void createIssue_shouldReturn401WithoutToken() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		CreateIssueRequest request = createIssueRequest(projectId, "Unauthorized issue test", IssueStatus.TODO);

		ResponseEntity<String> response = restTemplate.postForEntity(
				issueUrl(orgId, projectId, ""),
				request,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void listIssues_shouldReturn200WithEmptyContent() {
		TestSetup setup = setupOrgAndProject();

		ResponseEntity<PagedIssueResponse> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), ""),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				PagedIssueResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().totalElements()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void listIssues_shouldFilterByStatus() {
		TestSetup setup = setupOrgAndProject();
		createIssue(setup, "Todo issue", IssueStatus.TODO);
		createIssue(setup, "In progress issue", IssueStatus.IN_PROGRESS);

		ResponseEntity<PagedIssueResponse> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "?status=TODO"),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				PagedIssueResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().content()).allSatisfy(issue ->
				assertThat(issue.status()).isEqualTo(IssueStatus.TODO));
	}

	@Test
	void getIssue_shouldReturn200() {
		TestSetup setup = setupOrgAndProject();
		IssueResponse createdIssue = createIssue(setup, "Get issue test", IssueStatus.TODO);

		ResponseEntity<IssueDetailResponse> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + createdIssue.id()),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				IssueDetailResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().id()).isEqualTo(createdIssue.id());
	}

	@Test
	void getIssue_shouldReturn404ForUnknownId() {
		TestSetup setup = setupOrgAndProject();

		ResponseEntity<String> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + UUID.randomUUID()),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updateIssue_shouldReturn200WithUpdatedFields() {
		TestSetup setup = setupOrgAndProject();
		IssueResponse createdIssue = createIssue(setup, "Original title", IssueStatus.TODO);
		IssueDetailResponse currentIssue = getIssue(setup, createdIssue.id());
		UpdateIssueRequest request = new UpdateIssueRequest(
				"Updated title",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				currentIssue.version());

		ResponseEntity<IssueResponse> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + createdIssue.id()),
				HttpMethod.PATCH,
				new HttpEntity<>(request, authHeaders(setup.accessToken())),
				IssueResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().title()).isEqualTo("Updated title");
	}

	@Test
	void deleteIssue_shouldReturn204() {
		TestSetup setup = setupOrgAndProject();
		IssueResponse createdIssue = createIssue(setup, "Delete issue test", IssueStatus.TODO);

		ResponseEntity<Void> deleteResponse = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + createdIssue.id()),
				HttpMethod.DELETE,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				Void.class);

		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + createdIssue.id()),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private TestSetup setupOrgAndProject() {
		String email = uniqueEmail();
		restTemplate.postForEntity(authUrl("/register"), registerRequest(email), UserResponse.class);
		ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
				authUrl("/login"),
				new LoginRequest(email, PASSWORD),
				AuthResponse.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(loginResponse.getBody()).isNotNull();
		String accessToken = loginResponse.getBody().accessToken();

		String orgSlug = "org-" + UUID.randomUUID();
		ResponseEntity<OrganizationResponse> organizationResponse = restTemplate.postForEntity(
				organizationUrl(""),
				new HttpEntity<>(
						new CreateOrganizationRequest("Test Organization " + orgSlug, orgSlug),
						authHeaders(accessToken)),
				OrganizationResponse.class);
		assertThat(organizationResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(organizationResponse.getBody()).isNotNull();
		assertThat(organizationResponse.getBody().id()).isNotNull();
		UUID orgId = organizationResponse.getBody().id();

		ResponseEntity<ProjectResponse> projectResponse = restTemplate.postForEntity(
				projectUrl(orgId, ""),
				new HttpEntity<>(
						new CreateProjectRequest("Test Project", uniqueProjectKey(), orgId),
						authHeaders(accessToken)),
				ProjectResponse.class);
		assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(projectResponse.getBody()).isNotNull();
		assertThat(projectResponse.getBody().id()).isNotNull();

		return new TestSetup(accessToken, orgId, projectResponse.getBody().id());
	}

	private IssueResponse createIssue(TestSetup setup, String title, IssueStatus status) {
		ResponseEntity<IssueResponse> response = restTemplate.postForEntity(
				issueUrl(setup.orgId(), setup.projectId(), ""),
				new HttpEntity<>(
						createIssueRequest(setup.projectId(), title, status),
						authHeaders(setup.accessToken())),
				IssueResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		return response.getBody();
	}

	private IssueDetailResponse getIssue(TestSetup setup, UUID issueId) {
		ResponseEntity<IssueDetailResponse> response = restTemplate.exchange(
				issueUrl(setup.orgId(), setup.projectId(), "/" + issueId),
				HttpMethod.GET,
				new HttpEntity<>(authHeaders(setup.accessToken())),
				IssueDetailResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		return response.getBody();
	}

	private CreateIssueRequest createIssueRequest(UUID projectId, String title, IssueStatus status) {
		return new CreateIssueRequest(
				projectId,
				title,
				"Integration test issue",
				status,
				IssuePriority.MEDIUM,
				IssueType.TASK,
				null,
				null,
				null,
				null,
				List.of());
	}

	private String uniqueProjectKey() {
		return "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
	}

	private String organizationUrl(String suffix) {
		return "http://localhost:" + port + "/api/v1/organizations" + suffix;
	}

	private String projectUrl(UUID orgId, String suffix) {
		return "http://localhost:" + port + "/api/v1/organizations/" + orgId + "/projects" + suffix;
	}

	private String issueUrl(UUID orgId, UUID projectId, String suffix) {
		return "http://localhost:" + port
				+ "/api/v1/organizations/" + orgId
				+ "/projects/" + projectId
				+ "/issues" + suffix;
	}

	private record TestSetup(String accessToken, UUID orgId, UUID projectId) {
	}
}
