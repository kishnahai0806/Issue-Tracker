package com.krish.issuetracker.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AnalyticsOverviewResponse(
		LiveIssueCountsDto live,
		@JsonInclude(JsonInclude.Include.ALWAYS)
		SnapshotSummaryDto lastSnapshot) {
}
