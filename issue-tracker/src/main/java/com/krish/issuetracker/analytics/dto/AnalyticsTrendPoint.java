package com.krish.issuetracker.analytics.dto;

import java.time.LocalDate;

public record AnalyticsTrendPoint(
		LocalDate snapshotDate,
		long totalIssues,
		long openIssues,
		long closedIssues,
		Double avgResolutionHours) {
}
