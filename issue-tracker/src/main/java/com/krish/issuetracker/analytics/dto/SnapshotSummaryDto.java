package com.krish.issuetracker.analytics.dto;

import java.time.LocalDate;
import java.util.Map;

public record SnapshotSummaryDto(
		LocalDate asOf,
		long totalIssues,
		long openIssues,
		long closedIssues,
		Double avgResolutionHours,
		Map<String, Long> issuesByPriority,
		Map<String, Long> issuesByType,
		Map<String, Long> issuesByAssignee) {

	public SnapshotSummaryDto {
		issuesByPriority = issuesByPriority == null ? Map.of() : Map.copyOf(issuesByPriority);
		issuesByType = issuesByType == null ? Map.of() : Map.copyOf(issuesByType);
		issuesByAssignee = issuesByAssignee == null ? Map.of() : Map.copyOf(issuesByAssignee);
	}
}
