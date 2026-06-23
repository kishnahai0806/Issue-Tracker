package com.krish.issuetracker.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krish.issuetracker.analytics.dto.AnalyticsBurndownResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsOverviewResponse;
import com.krish.issuetracker.analytics.dto.AnalyticsTrendsResponse;
import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.repository.AnalyticsSnapshotRepository;
import com.krish.issuetracker.repository.IssueRepository;
import com.krish.issuetracker.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

	@Mock
	private AnalyticsSnapshotRepository analyticsSnapshotRepository;

	@Mock
	private IssueRepository issueRepository;

	@Mock
	private ProjectRepository projectRepository;

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	private AnalyticsService analyticsService;

	@Test
	void getProjectOverview_shouldReturnLiveCountsWithSnapshot_whenSnapshotExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.countByProjectId(projectId)).thenReturn(10L);
		when(issueRepository.countOpenByProjectId(projectId)).thenReturn(6L);
		when(issueRepository.countInProgressByProjectId(projectId)).thenReturn(2L);
		when(issueRepository.countClosedByProjectId(projectId)).thenReturn(4L);
		AnalyticsSnapshot snapshot = snapshot(projectId, LocalDate.of(2026, 1, 1));
		snapshot.setIssuesByPriority("{\"HIGH\":2}");
		when(analyticsSnapshotRepository.findTopByProjectIdOrderBySnapshotDateDesc(projectId))
				.thenReturn(Optional.of(snapshot));

		AnalyticsOverviewResponse response = analyticsService.getProjectOverview(orgId, projectId);

		assertThat(response.live().totalIssues()).isEqualTo(10L);
		assertThat(response.lastSnapshot()).isNotNull();
		assertThat(response.lastSnapshot().issuesByPriority()).containsEntry("HIGH", 2L);
	}

	@Test
	void getProjectOverview_shouldReturnPendingSnapshot_whenNoSnapshotExists() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(issueRepository.countByProjectId(projectId)).thenReturn(0L);
		when(issueRepository.countOpenByProjectId(projectId)).thenReturn(0L);
		when(issueRepository.countInProgressByProjectId(projectId)).thenReturn(0L);
		when(issueRepository.countClosedByProjectId(projectId)).thenReturn(0L);
		when(analyticsSnapshotRepository.findTopByProjectIdOrderBySnapshotDateDesc(projectId))
				.thenReturn(Optional.empty());

		AnalyticsOverviewResponse response = analyticsService.getProjectOverview(orgId, projectId);

		assertThat(response.lastSnapshot()).isNull();
	}

	@Test
	void getProjectTrends_shouldReturnPointsWithinDateRange_whenSnapshotsExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to))
				.thenReturn(List.of(snapshot(projectId, from)));

		AnalyticsTrendsResponse response = analyticsService.getProjectTrends(orgId, projectId, from, to);

		assertThat(response.points()).hasSize(1);
		assertThat(response.points().get(0).snapshotDate()).isEqualTo(from);
	}

	@Test
	void getProjectTrends_shouldReturnEmptyList_whenNoSnapshotsExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to))
				.thenReturn(List.of());

		AnalyticsTrendsResponse response = analyticsService.getProjectTrends(orgId, projectId, from, to);

		assertThat(response.points()).isEmpty();
	}

	@Test
	void getProjectBurndown_shouldReturnPointsWithinDateRange_whenSnapshotsExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to))
				.thenReturn(List.of(snapshot(projectId, from)));

		AnalyticsBurndownResponse response = analyticsService.getProjectBurndown(orgId, projectId, from, to);

		assertThat(response.points()).hasSize(1);
		assertThat(response.points().get(0).snapshotDate()).isEqualTo(from);
	}

	@Test
	void getProjectBurndown_shouldReturnEmptyList_whenNoSnapshotsExist() {
		UUID orgId = UUID.randomUUID();
		UUID projectId = UUID.randomUUID();
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		when(projectRepository.findByIdAndOrganizationIdAndIsArchivedFalse(projectId, orgId))
				.thenReturn(Optional.of(project(projectId, orgId)));
		when(analyticsSnapshotRepository
				.findAllByProjectIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(projectId, from, to))
				.thenReturn(List.of());

		AnalyticsBurndownResponse response = analyticsService.getProjectBurndown(orgId, projectId, from, to);

		assertThat(response.points()).isEmpty();
	}

	private Project project(UUID projectId, UUID orgId) {
		Project project = new Project();
		project.setId(projectId);
		project.setOrganizationId(orgId);
		project.setName("Test Project");
		project.setKey("TEST");
		return project;
	}

	private AnalyticsSnapshot snapshot(UUID projectId, LocalDate date) {
		AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
		snapshot.setId(UUID.randomUUID());
		snapshot.setProjectId(projectId);
		snapshot.setSnapshotDate(date);
		snapshot.setTotalIssues(10);
		snapshot.setOpenIssues(6);
		snapshot.setClosedIssues(4);
		snapshot.setAvgResolutionHours(12.5);
		return snapshot;
	}
}
