package com.krish.issuetracker.issue;

import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.issue.dto.CreateIssueRequest;
import com.krish.issuetracker.issue.dto.IssueDetailResponse;
import com.krish.issuetracker.issue.dto.IssueFilterRequest;
import com.krish.issuetracker.issue.dto.IssueResponse;
import com.krish.issuetracker.issue.dto.PagedIssueResponse;
import com.krish.issuetracker.issue.dto.UpdateIssueRequest;
import com.krish.issuetracker.issue.dto.WatcherRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/projects/{projectId}/issues")
@Validated
public class IssueController {

	private final IssueService issueService;

	public IssueController(IssueService issueService) {
		this.issueService = issueService;
	}

	@PostMapping
	public ResponseEntity<IssueResponse> createIssue(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateIssueRequest request,
			Authentication authentication) {
		// Prevents a request body projectId from silently overriding the authenticated path projectId.
		if (!projectId.equals(request.projectId())) {
			throw new IllegalArgumentException("Path projectId does not match request projectId");
		}

		IssueResponse response = issueService.createIssue(request, getAuthenticatedUserId(authentication), orgId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<PagedIssueResponse> listIssues(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@RequestParam(required = false) IssueStatus status,
			@RequestParam(required = false) IssuePriority priority,
			@RequestParam(required = false) IssueType type,
			@RequestParam(required = false) UUID assigneeId,
			@RequestParam(required = false) UUID labelId,
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		IssueFilterRequest filter = new IssueFilterRequest(
				projectId,
				status,
				priority,
				type,
				assigneeId,
				labelId,
				search,
				page,
				size);
		return ResponseEntity.ok(issueService.listIssues(filter, orgId));
	}

	@GetMapping("/{issueId}")
	public ResponseEntity<IssueDetailResponse> getIssue(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			Authentication authentication) {
		IssueDetailResponse response = issueService.getIssue(
				orgId,
				projectId,
				issueId,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{issueId}")
	public ResponseEntity<IssueResponse> updateIssue(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@Valid @RequestBody UpdateIssueRequest request,
			Authentication authentication) {
		IssueResponse response = issueService.updateIssue(
				orgId,
				projectId,
				issueId,
				request,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{issueId}")
	public ResponseEntity<Void> deleteIssue(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			Authentication authentication) {
		issueService.deleteIssue(orgId, projectId, issueId, getAuthenticatedUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{issueId}/watchers")
	public ResponseEntity<Void> addWatcher(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@Valid @RequestBody WatcherRequest request) {
		issueService.addWatcher(orgId, projectId, issueId, request.userId());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{issueId}/watchers/{userId}")
	public ResponseEntity<Void> removeWatcher(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@PathVariable UUID userId) {
		issueService.removeWatcher(orgId, projectId, issueId, userId);
		return ResponseEntity.noContent().build();
	}

	private UUID getAuthenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}
}
