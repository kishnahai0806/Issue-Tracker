package com.krish.issuetracker.comment;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.issue.dto.CommentResponse;
import com.krish.issuetracker.issue.dto.CreateCommentRequest;
import com.krish.issuetracker.issue.dto.UpdateCommentRequest;
import com.krish.issuetracker.security.AuthenticatedUser;
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
@RequestMapping("/api/v1/organizations/{orgId}/projects/{projectId}/issues/{issueId}/comments")
@Validated
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	@PostMapping
	public ResponseEntity<CommentResponse> addComment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@Valid @RequestBody CreateCommentRequest request,
			Authentication authentication) {
		CommentResponse response = commentService.addComment(
				orgId,
				projectId,
				issueId,
				request,
				AuthenticatedUser.id(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<CommentResponse>> getComments(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId) {
		return ResponseEntity.ok(commentService.getComments(orgId, projectId, issueId));
	}

	@PatchMapping("/{commentId}")
	public ResponseEntity<CommentResponse> updateComment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@PathVariable UUID commentId,
			@Valid @RequestBody UpdateCommentRequest request,
			Authentication authentication) {
		CommentResponse response = commentService.updateComment(
				orgId,
				projectId,
				issueId,
				commentId,
				request,
				AuthenticatedUser.id(authentication));
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{commentId}")
	public ResponseEntity<Void> deleteComment(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID issueId,
			@PathVariable UUID commentId,
			Authentication authentication) {
		commentService.deleteComment(orgId, projectId, issueId, commentId, AuthenticatedUser.id(authentication));
		return ResponseEntity.noContent().build();
	}
}
