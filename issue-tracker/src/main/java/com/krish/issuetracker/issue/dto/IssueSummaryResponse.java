package com.krish.issuetracker.issue.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;

public record IssueSummaryResponse(
		UUID id,
		UUID projectId,
		int issueNumber,
		String title,
		IssueStatus status,
		IssuePriority priority,
		IssueType type,
		UUID assigneeId,
		LocalDate dueDate,
		LocalDateTime updatedAt) {
}
