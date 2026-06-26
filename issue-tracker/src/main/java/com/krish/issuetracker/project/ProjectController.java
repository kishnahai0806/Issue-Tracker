package com.krish.issuetracker.project;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.project.dto.CreateProjectRequest;
import com.krish.issuetracker.project.dto.ProjectResponse;
import com.krish.issuetracker.project.dto.ProjectSummaryResponse;
import com.krish.issuetracker.project.dto.UpdateProjectRequest;
import com.krish.issuetracker.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/projects")
@Validated
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@PostMapping
	public ResponseEntity<ProjectResponse> createProject(
			@PathVariable UUID orgId,
			@Valid @RequestBody CreateProjectRequest request,
			Authentication authentication) {
		ProjectResponse response = projectService.createProject(orgId, request, AuthenticatedUser.id(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ProjectSummaryResponse>> listProjects(@PathVariable UUID orgId) {
		return ResponseEntity.ok(projectService.listProjects(orgId));
	}

	@GetMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> getProject(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId) {
		return ResponseEntity.ok(projectService.getProject(orgId, projectId));
	}

	@PatchMapping("/{projectId}")
	public ResponseEntity<ProjectResponse> updateProject(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@Valid @RequestBody UpdateProjectRequest request) {
		ProjectResponse response = projectService.updateProject(orgId, projectId, request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{projectId}/archive")
	public ResponseEntity<ProjectResponse> archiveProject(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId) {
		ProjectResponse response = projectService.archiveProject(orgId, projectId);
		return ResponseEntity.ok(response);
	}

}
