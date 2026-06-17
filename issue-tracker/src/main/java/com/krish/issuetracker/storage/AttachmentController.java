package com.krish.issuetracker.storage;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.storage.dto.AttachmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/projects/{projectId}/issues/{issueId}/attachments")
@Validated
public class AttachmentController {

	private final AttachmentService attachmentService;

	public AttachmentController(AttachmentService attachmentService) {
		this.attachmentService = attachmentService;
	}

	@PostMapping
	public ResponseEntity<AttachmentResponse> uploadAttachment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@RequestParam("file") MultipartFile file,
			Authentication authentication) {
		AttachmentResponse response = attachmentService.uploadAttachment(
				orgId,
				projectId,
				issueId,
				file,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<AttachmentResponse>> listAttachments(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId) {
		return ResponseEntity.ok(attachmentService.listAttachments(orgId, projectId, issueId));
	}

	@GetMapping("/{attachmentId}/download")
	public ResponseEntity<Void> downloadAttachment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@PathVariable UUID attachmentId) {
		String presignedUrl = attachmentService.getPresignedDownloadUrl(orgId, projectId, issueId, attachmentId);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(presignedUrl))
				.build();
	}

	@DeleteMapping("/{attachmentId}")
	public ResponseEntity<Void> deleteAttachment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@PathVariable UUID attachmentId,
			Authentication authentication) {
		attachmentService.deleteAttachment(
				orgId,
				projectId,
				issueId,
				attachmentId,
				getAuthenticatedUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	private UUID getAuthenticatedUserId(Authentication authentication) {
		return UUID.fromString(authentication.getName());
	}
}
