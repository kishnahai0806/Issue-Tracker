package com.krish.issuetracker.batch;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import com.krish.issuetracker.domain.entity.Project;
import com.krish.issuetracker.repository.IssueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyticsSnapshotProcessor implements ItemProcessor<Project, AnalyticsSnapshot> {

	private final IssueRepository issueRepository;
	private final ObjectMapper objectMapper;

	public AnalyticsSnapshotProcessor(IssueRepository issueRepository, ObjectMapper objectMapper) {
		this.issueRepository = issueRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	public AnalyticsSnapshot process(Project project) throws Exception {
		AnalyticsSnapshot snapshot = new AnalyticsSnapshot();
		snapshot.setProjectId(project.getId());
		snapshot.setSnapshotDate(LocalDate.now());
		snapshot.setTotalIssues(toInteger(issueRepository.countByProjectId(project.getId())));
		snapshot.setOpenIssues(toInteger(issueRepository.countOpenByProjectId(project.getId())));
		snapshot.setClosedIssues(toInteger(issueRepository.countClosedByProjectId(project.getId())));
		snapshot.setAvgResolutionHours(issueRepository.avgResolutionHoursByProjectId(project.getId()));
		snapshot.setIssuesByPriority(toJson(issueRepository.countByPriorityForProject(project.getId())));
		snapshot.setIssuesByType(toJson(issueRepository.countByTypeForProject(project.getId())));
		snapshot.setIssuesByAssignee(toJson(issueRepository.countByAssigneeForProject(project.getId())));

		log.debug("Analytics snapshot processed for project {}", project.getId());
		return snapshot;
	}

	private String toJson(List<Object[]> rows) throws Exception {
		Map<String, Long> values = new LinkedHashMap<>();
		for (Object[] row : rows) {
			values.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
		}
		return objectMapper.writeValueAsString(values);
	}

	private Integer toInteger(long value) {
		return Math.toIntExact(value);
	}
}
