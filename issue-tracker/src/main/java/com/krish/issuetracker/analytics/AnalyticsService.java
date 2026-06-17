package com.krish.issuetracker.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krish.issuetracker.analytics.dto.AnalyticsBurndownResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsOverviewResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsTrendPoint;
import com.krish.issuetracker.analytics.dto.AnalyticsTrendsResponse;
import com.krish.issuetracker.analytics.dto.BurndownPoint;
import com.krish.issuetracker.analytics.dto.LiveIssueCountsDto;
import com.krish.issuetracker.analytics.dto.SnapshotSummaryDto;
import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import com.krish.issuetracker.exception.ProjectNotFoundException;
import com.krish.issuetracker.repository.AnalyticsSnapshotRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AnalyticsService {

	private static final TypeReference<Map<String, Long>> JSON_MAP_TYPE = new TypeReference<>() {
	};

	private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
	private final IssueRepository issueRepository;
	private final ProjectRepository projectRepository;
	private final ObjectMapper objectMapper;

	public AnalyticsService(
			AnalyticsSnapshotRepository analyticsSnapshotRepository,
			IssueRepository issueRepository,
			ProjectRepository projectRepository,
			ObjectMapper objectMapper) {
		this.analyticsSnapshotRepository = analyticsSnapshotRepository;
		this.issueRepository = issueRepository;
		this.projectRepository = projectRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	public AnalyticsOverviewResponse getProjectOverview(UUID orgId, UUID projectId) {
		verifyProjectAccess(orgId, projectId);

		LiveIssueCountsDto live = new LiveIssueCountsDto(
				Instant.now(),
				issueRepository.countByProjectId(projectId),
				issueRepository.countOpenByProjectId(projectId),
				issueRepository.countInProgressByProjectId(projectId),
				issueRepository.countClosedByProjectId(projectId));

		SnapshotSummaryDto lastSnapshot = analyticsSnapshotRepository
				.findTopByProjectIdOrderBySnapshotDateDesc(projectId)
				.map(this::parseSnapshotSummary)
				.orElse(null);

		return new AnalyticsOverviewResponse(live, lastSnapshot);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	@Cacheable(value = "analytics-trends", key = "#projectId + '_' + #from + '_' + #to")
	public AnalyticsTrendsResponse getProjectTrends(UUID orgId, UUID projectId, LocalDate from, LocalDate to) {
		verifyProjectAccess(orgId, projectId);
		List<AnalyticsTrendPoint> points = analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to)
				.stream()
				.map(snapshot -> new AnalyticsTrendPoint(
						snapshot.getSnapshotDate(),
						snapshot.getTotalIssues(),
						snapshot.getOpenIssues(),
						snapshot.getClosedIssues(),
						snapshot.getAvgResolutionHours()))
				.toList();

		return new AnalyticsTrendsResponse(projectId, from, to, points);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'REPORTER')")
	@Cacheable(value = "analytics-burndown", key = "#projectId + '_' + #from + '_' + #to")
	public AnalyticsBurndownResponse getProjectBurndown(UUID orgId, UUID projectId, LocalDate from, LocalDate to) {
		verifyProjectAccess(orgId, projectId);
		List<BurndownPoint> points = analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to)
				.stream()
				.map(snapshot -> new BurndownPoint(
						snapshot.getSnapshotDate(),
						snapshot.getOpenIssues(),
						snapshot.getClosedIssues()))
				.toList();

		return new AnalyticsBurndownResponse(projectId, from, to, points);
	}

	private SnapshotSummaryDto parseSnapshotSummary(AnalyticsSnapshot snapshot) {
		return new SnapshotSummaryDto(
				snapshot.getSnapshotDate(),
				snapshot.getTotalIssues(),
				snapshot.getOpenIssues(),
				snapshot.getClosedIssues(),
				snapshot.getAvgResolutionHours(),
				parseJsonMap(snapshot.getIssuesByPriority(), "issuesByPriority"),
				parseJsonMap(snapshot.getIssuesByType(), "issuesByType"),
				parseJsonMap(snapshot.getIssuesByAssignee(), "issuesByAssignee"));
	}

	private Map<String, Long> parseJsonMap(String json, String fieldName) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}

		try {
			return objectMapper.readValue(json, JSON_MAP_TYPE);
		} catch (JsonProcessingException ex) {
			log.warn("Failed to parse analytics snapshot field {}", fieldName, ex);
			return Map.of();
		}
	}

	private void verifyProjectAccess(UUID orgId, UUID projectId) {
		projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId)
				.orElseThrow(() -> new ProjectNotFoundException(projectId));
	}
}
