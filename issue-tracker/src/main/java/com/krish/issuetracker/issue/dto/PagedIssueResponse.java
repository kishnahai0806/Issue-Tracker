package com.krish.issuetracker.issue.dto;

import java.util.List;

public record PagedIssueResponse(
		List<IssueSummaryResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean isLast) {
}
