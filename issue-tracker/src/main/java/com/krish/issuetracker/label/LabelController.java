package com.krish.issuetracker.label;

import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.label.dto.CreateLabelRequest;
import com.krish.issuetracker.label.dto.LabelResponse;
import com.krish.issuetracker.label.dto.UpdateLabelRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/organizations/{orgId}/projects/{projectId}/labels")
@Validated
public class LabelController {

	private final LabelService labelService;

	public LabelController(LabelService labelService) {
		this.labelService = labelService;
	}

	@PostMapping
	public ResponseEntity<LabelResponse> createLabel(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@Valid @RequestBody CreateLabelRequest request) {
		LabelResponse response = labelService.createLabel(orgId, projectId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<LabelResponse>> listLabels(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId) {
		return ResponseEntity.ok(labelService.listLabels(orgId, projectId));
	}

	@PatchMapping("/{labelId}")
	public ResponseEntity<LabelResponse> updateLabel(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID labelId,
			@Valid @RequestBody UpdateLabelRequest request) {
		LabelResponse response = labelService.updateLabel(orgId, projectId, labelId, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{labelId}")
	public ResponseEntity<Void> deleteLabel(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@PathVariable UUID labelId) {
		labelService.deleteLabel(orgId, projectId, labelId);
		return ResponseEntity.noContent().build();
	}
}
