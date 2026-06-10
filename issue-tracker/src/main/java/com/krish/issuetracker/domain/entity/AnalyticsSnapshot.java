package com.krish.issuetracker.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analytics_snapshots")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnalyticsSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "snapshot_date", nullable = false)
	private LocalDate snapshotDate;

	@Column(name = "total_issues", nullable = false)
	private Integer totalIssues = 0;

	@Column(name = "open_issues", nullable = false)
	private Integer openIssues = 0;

	@Column(name = "closed_issues", nullable = false)
	private Integer closedIssues = 0;

	@Column(name = "avg_resolution_hours")
	private Double avgResolutionHours;

	@Column(name = "issues_by_priority", columnDefinition = "jsonb")
	private String issuesByPriority;

	@Column(name = "issues_by_type", columnDefinition = "jsonb")
	private String issuesByType;

	@Column(name = "issues_by_assignee", columnDefinition = "jsonb")
	private String issuesByAssignee;

	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
