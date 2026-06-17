package com.krish.issuetracker.analytics.dto;

import java.time.Instant;

public record LiveIssueCountsDto(
		Instant asOf,
		long totalIssues,
		long openIssues,
		long inProgressIssues,
		long closedIssues) {
}
