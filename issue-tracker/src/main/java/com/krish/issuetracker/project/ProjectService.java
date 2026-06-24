package com.krish.issuetracker.project;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.exception.OrganizationNotFoundException;
import com.krish.issuetracker.exception.ProjectKeyAlreadyExistsException;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.project.dto.CreateProjectRequest;
import com.krish.issuetracker.project.dto.ProjectResponse;
import com.krish.issuetracker.project.dto.ProjectSummaryResponse;
import com.krish.issuetracker.project.dto.UpdateProjectRequest;
import com.krish.issuetracker.repository.OrganizationRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final OrganizationRepository organizationRepository;

	public ProjectService(ProjectRepository projectRepository, OrganizationRepository organizationRepository) {
		this.projectRepository = projectRepository;
		this.organizationRepository = organizationRepository;
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public ProjectResponse createProject(UUID orgId, CreateProjectRequest request, UUID creatorUserId) {
		organizationRepository.findById(orgId)
				.orElseThrow(() -> new OrganizationNotFoundException(orgId));

		if (projectRepository.existsByKeyAndOrganizationId(request.key(), orgId)) {
			throw new ProjectKeyAlreadyExistsException(request.key(), orgId);
		}

		Project project = new Project();
		project.setOrganizationId(orgId);
		project.setName(request.name());
		project.setKey(request.key());
		project.setDescription(request.description());
		project.setCreatedBy(creatorUserId);
		project.setArchived(false);

		Project savedProject = projectRepository.save(project);
		log.info("Project created: {} in org {}", savedProject.getId(), orgId);

		return toProjectResponse(savedProject);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public ProjectResponse getProject(UUID orgId, UUID projectId) {
		return toProjectResponse(loadActiveProject(orgId, projectId));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public List<ProjectSummaryResponse> listProjects(UUID orgId) {
		return projectRepository.findAllByOrganizationIdAndIsArchivedFalse(orgId)
				.stream()
				.map(this::toProjectSummaryResponse)
				.toList();
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public ProjectResponse updateProject(
			UUID orgId,
			UUID projectId,
			UpdateProjectRequest request) {
		Project project = loadActiveProject(orgId, projectId);

		if (request.name() != null) {
			project.setName(request.name());
		}

		return toProjectResponse(projectRepository.save(project));
	}

	@Transactional
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT_MANAGER')")
	public ProjectResponse archiveProject(UUID orgId, UUID projectId) {
		Project project = loadActiveProject(orgId, projectId);
		project.setArchived(true);

		Project savedProject = projectRepository.save(project);
		log.info("Project archived: {}", projectId);

		return toProjectResponse(savedProject);
	}

	private Project loadActiveProject(UUID orgId, UUID projectId) {
		return projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}

	private ProjectResponse toProjectResponse(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getOrganizationId(),
				project.getName(),
				project.getKey(),
				project.getCreatedBy(),
				project.isArchived(),
				project.getCreatedAt(),
				project.getUpdatedAt());
	}

	private ProjectSummaryResponse toProjectSummaryResponse(Project project) {
		return new ProjectSummaryResponse(
				project.getId(),
				project.getOrganizationId(),
				project.getName(),
				project.getKey(),
				project.isArchived());
	}
}
