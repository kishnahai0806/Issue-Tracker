package com.krish.issuetracker.organization;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.organization.dto.AddMemberRequest;
import com.krish.issuetracker.organization.dto.CreateOrganizationRequest;
import com.krish.issuetracker.organization.dto.MemberResponse;
import com.krish.issuetracker.organization.dto.OrganizationResponse;
import com.krish.issuetracker.organization.dto.OrganizationSummaryResponse;
import com.krish.issuetracker.organization.dto.UpdateMemberRoleRequest;
import com.krish.issuetracker.organization.dto.UpdateOrganizationRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Validated
public class OrganizationController {

	private final OrganizationService organizationService;

	public OrganizationController(OrganizationService organizationService) {
		this.organizationService = organizationService;
	}

	@PostMapping
	public ResponseEntity<OrganizationResponse> createOrganization(
			@Valid @RequestBody CreateOrganizationRequest request,
			Authentication authentication) {
		OrganizationResponse response = organizationService.createOrganization(
				request,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<OrganizationSummaryResponse>> listOrganizations(Authentication authentication) {
		return ResponseEntity.ok(organizationService.listOrganizations(getAuthenticatedUserId(authentication)));
	}

	@GetMapping("/{orgId}")
	public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable UUID orgId) {
		return ResponseEntity.ok(organizationService.getOrganization(orgId));
	}

	@PatchMapping("/{orgId}")
	public ResponseEntity<OrganizationResponse> updateOrganization(
			@PathVariable UUID orgId,
			@Valid @RequestBody UpdateOrganizationRequest request,
			Authentication authentication) {
		OrganizationResponse response = organizationService.updateOrganization(
				orgId,
				request,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{orgId}/members")
	public ResponseEntity<MemberResponse> addMember(
			@PathVariable UUID orgId,
			@Valid @RequestBody AddMemberRequest request,
			Authentication authentication) {
		MemberResponse response = organizationService.addMember(
				orgId,
				request,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/{orgId}/members")
	public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable UUID orgId) {
		return ResponseEntity.ok(organizationService.listMembers(orgId));
	}

	@PatchMapping("/{orgId}/members/{userId}")
	public ResponseEntity<MemberResponse> updateMemberRole(
			@PathVariable UUID orgId,
			@PathVariable UUID userId,
			@Valid @RequestBody UpdateMemberRoleRequest request,
			Authentication authentication) {
		MemberResponse response = organizationService.updateMemberRole(
				orgId,
				userId,
				request,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{orgId}/members/{userId}")
	public ResponseEntity<Void> removeMember(
			@PathVariable UUID orgId,
			@PathVariable UUID userId,
			Authentication authentication) {
		organizationService.removeMember(orgId, userId, getAuthenticatedUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	private UUID getAuthenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}
}
