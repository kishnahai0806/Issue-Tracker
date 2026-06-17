package com.krish.issuetracker.issue.dto;

import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record IssueFilterRequest(
		@NotNull
		UUID projectId,

		IssueStatus status,

		IssuePriority priority,

		IssueType type,

		UUID assigneeId,

		UUID labelId,

		String search,

		@Min(0)
		int page,

		@Min(1)
		@Max(100)
		int size) {

	public IssueFilterRequest {
		if (page < 0) {
			page = 0;
		}
		if (size == 0) {
			size = 20;
		}
	}
}
