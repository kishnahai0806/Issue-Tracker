package com.krish.issuetracker.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AnalyticsBurndownResponse(
		UUID projectId,
		LocalDate from,
		LocalDate to,
		List<BurndownPoint> points) {

	public AnalyticsBurndownResponse {
		points = points == null ? List.of() : List.copyOf(points);
	}
}
