package com.krish.issuetracker.analytics.dto;

import java.time.LocalDate;

public record BurndownPoint(
		LocalDate snapshotDate,
		long openIssues,
		long closedIssues) {
}
