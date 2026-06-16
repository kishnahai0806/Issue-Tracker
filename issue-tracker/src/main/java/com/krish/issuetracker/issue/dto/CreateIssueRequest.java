package com.krish.issuetracker.issue.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
		@NotNull
		UUID projectId,

		@NotBlank
		@Size(max = 500)
		String title,

		String description,

		@NotNull
		IssueStatus status,

		@NotNull
		IssuePriority priority,

		@NotNull
		IssueType type,

		UUID assigneeId,

		UUID parentIssueId,

		@Min(0)
		@Max(100)
		Integer storyPoints,

		LocalDate dueDate,

		List<UUID> labelIds) {

	public CreateIssueRequest {
		if (labelIds == null) {
			labelIds = List.of();
		}
	}
}
