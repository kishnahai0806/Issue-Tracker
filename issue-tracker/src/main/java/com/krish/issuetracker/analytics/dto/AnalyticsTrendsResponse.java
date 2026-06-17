package com.krish.issuetracker.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AnalyticsTrendsResponse(
		UUID projectId,
		LocalDate from,
		LocalDate to,
		List<AnalyticsTrendPoint> points) {

	public AnalyticsTrendsResponse {
		points = points == null ? List.of() : List.copyOf(points);
	}
}
