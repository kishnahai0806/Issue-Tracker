package com.krish.issuetracker.batch;

import java.util.List;

import javax.sql.DataSource;

import com.krish.issuetracker.domain.entity.AnalyticsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyticsSnapshotWriter implements ItemWriter<AnalyticsSnapshot> {

	private static final String UPSERT_SQL = """
			INSERT INTO analytics_snapshots (
				project_id,
				snapshot_date,
				total_issues,
				open_issues,
				closed_issues,
				avg_resolution_hours,
				issues_by_priority,
				issues_by_type,
				issues_by_assignee
			)
			VALUES (
				:projectId,
				:snapshotDate,
				:totalIssues,
				:openIssues,
				:closedIssues,
				:avgResolutionHours,
				CAST(:issuesByPriority AS jsonb),
				CAST(:issuesByType AS jsonb),
				CAST(:issuesByAssignee AS jsonb)
			)
			ON CONFLICT (project_id, snapshot_date)
			DO UPDATE SET
				total_issues = EXCLUDED.total_issues,
				open_issues = EXCLUDED.open_issues,
				closed_issues = EXCLUDED.closed_issues,
				avg_resolution_hours = EXCLUDED.avg_resolution_hours,
				issues_by_priority = EXCLUDED.issues_by_priority::jsonb,
				issues_by_type = EXCLUDED.issues_by_type::jsonb,
				issues_by_assignee = EXCLUDED.issues_by_assignee::jsonb
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public AnalyticsSnapshotWriter(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends AnalyticsSnapshot> chunk) {
		List<? extends AnalyticsSnapshot> snapshots = chunk.getItems();
		SqlParameterSource[] parameters = snapshots.stream()
				.map(BeanPropertySqlParameterSource::new)
				.toArray(SqlParameterSource[]::new);

		jdbcTemplate.batchUpdate(UPSERT_SQL, parameters);
		log.debug("Analytics snapshots written: {}", snapshots.size());
	}
}
