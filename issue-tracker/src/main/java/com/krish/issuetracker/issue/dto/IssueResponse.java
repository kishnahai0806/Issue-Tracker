package com.krish.issuetracker.issue.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import com.krish.issuetracker.label.dto.LabelResponse;

public record IssueResponse(
		UUID id,
		UUID projectId,
		int issueNumber,
		String title,
		String description,
		IssueStatus status,
		IssuePriority priority,
		IssueType type,
		UUID reporterId,
		UUID assigneeId,
		UUID parentIssueId,
		Integer storyPoints,
		LocalDate dueDate,
		LocalDateTime resolvedAt,
		LocalDateTime closedAt,
		Long version,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<LabelResponse> labels) {
}
