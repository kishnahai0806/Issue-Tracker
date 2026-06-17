package com.krish.issuetracker.issue.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIssueRequest(
		@Size(max = 500)
		String title,

		String description,

		IssueStatus status,

		IssuePriority priority,

		IssueType type,

		UUID assigneeId,

		@Min(0)
		@Max(100)
		Integer storyPoints,

		LocalDate dueDate,

		@NotNull
		Long version) {
}
