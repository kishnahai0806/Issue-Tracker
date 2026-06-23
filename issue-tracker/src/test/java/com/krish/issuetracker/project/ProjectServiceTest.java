package com.krish.issuetracker.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Organization;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.exception.OrganizationNotFoundException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.project.dto.CreateProjectRequest;
import com.krish.issuetracker.project.dto.ProjectResponse;
import com.krish.issuetracker.project.dto.ProjectSummaryResponse;
import com.krish.issuetracker.project.dto.UpdateProjectRequest;
import com.krish.issuetracker.repository.OrganizationRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private OrganizationRepository organizationRepository;

	@InjectMocks
	private ProjectService projectService;

	@Test
	void createProject_shouldSaveProject_whenOrganizationExists() {
		UUID orgId = UUID.randomUUID();
		UUID creatorId = UUID.randomUUID();
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization(orgId)));
		when(projectRepository.existsByKeyAndOrganizationId("PROJ", orgId)).thenReturn(false);
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
			Project project = invocation.getArgument(0);
			project.setId(UUID.randomUUID());
			return project;
		});

		ProjectResponse response = projectService.createProject(
				orgId, new CreateProjectRequest("Test Project", "PROJ", "description"), creatorId);

		assertThat(response).isNotNull();
		assertThat(response.key()).isEqualTo("PROJ");
		assertThat(response.organizationId()).isEqualTo(orgId);
	}

	@Test
	void createProject_shouldThrowOrganizationNotFoundException_whenOrganizationDoesNotExist() {
		UUID orgId = UUID.randomUUID();
		when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.createProject(
				orgId, new CreateProjectRequest("Test Project", "PROJ", "description"), UUID.randomUUID()))
				.isInstanceOf(OrganizationNotFoundException.class);
	}

	@Test
	void listProjects_shouldReturnProjectsForOrganization() {
		UUID orgId = UUID.randomUUID();
		Project project = project(UUID.randomUUID(), orgId, "PROJ");
		when(projectRepository.findAllByOrganizationIdAndIsArchivedFalse(orgId)).thenReturn(List.of(project));

		List<ProjectSummaryResponse> responses = projectService.listProjects(orgId);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).organizationId()).isEqualTo(orgId);
	}

	@Test
	void getProject_shouldReturnProject_whenExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId, orgId, "PROJ");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));

		ProjectResponse response = projectService.getProject(orgId, projectId);

		assertThat(response.id()).isEqualTo(projectId);
	}

	@Test
	void getProject_shouldThrowProjectNotFoundException_whenNotFound() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> projectService.getProject(orgId, projectId))
				.isInstanceOf(ProjectNotFoundException.class);
	}

	@Test
	void updateProject_shouldUpdateName_whenProjectExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId, orgId, "PROJ");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProjectResponse response = projectService.updateProject(
				orgId, projectId, new UpdateProjectRequest("Renamed Project"), UUID.randomUUID());

		assertThat(response.name()).isEqualTo("Renamed Project");
	}

	@Test
	void archiveProject_shouldArchiveProject_whenProjectExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		Project project = project(projectId, orgId, "PROJ");
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project));
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProjectResponse response = projectService.archiveProject(orgId, projectId, UUID.randomUUID());

		assertThat(response.isArchived()).isTrue();
	}

	private Organization organization(UUID orgId) {
		Organization organization = new Organization();
		organization.setId(orgId);
		organization.setName("Acme");
		organization.setSlug("acme");
		return organization;
	}

	private Project project(UUID projectId, UUID orgId, String key) {
		Project project = new Project();
		project.setId(projectId);
		project.setOrganizationId(orgId);
		project.setName("Test Project");
		project.setKey(key);
		return project;
	}
}
