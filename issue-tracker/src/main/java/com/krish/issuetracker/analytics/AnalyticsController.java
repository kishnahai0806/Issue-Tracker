package com.krish.issuetracker.analytics;

import java.time.LocalDate;
import java.util.UUID;

import com.krish.issuetracker.analytics.dto.AnalyticsBurndownResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsOverviewResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsTrendsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/projects/{projectId}/analytics")
@Validated
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/overview")
	public ResponseEntity<AnalyticsOverviewResponse> getOverview(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			Authentication authentication) {
		return ResponseEntity.ok(analyticsService.getProjectOverview(orgId, projectId));
	}

	@GetMapping("/trends")
	public ResponseEntity<AnalyticsTrendsResponse> getTrends(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		validateDateRange(from, to);
		return ResponseEntity.ok(analyticsService.getProjectTrends(orgId, projectId, from, to));
	}

	@GetMapping("/burndown")
	public ResponseEntity<AnalyticsBurndownResponse> getBurndown(
			@PathVariable UUID orgId,
			@PathVariable UUID projectId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		validateDateRange(from, to);
		return ResponseEntity.ok(analyticsService.getProjectBurndown(orgId, projectId, from, to));
	}

	private void validateDateRange(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from date must not be after to date");
		}
	}
}
