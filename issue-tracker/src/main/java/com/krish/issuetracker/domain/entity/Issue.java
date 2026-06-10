package com.krish.issuetracker.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.IssuePriority;
import com.krish.issuetracker.domain.enums.IssueStatus;
import com.krish.issuetracker.domain.enums.IssueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Issue {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(name = "project_id", nullable = false)
	private UUID projectId;

	@Column(name = "issue_number", nullable = false)
	private Integer issueNumber;

	@Column(name = "title", nullable = false, length = 500)
	private String title;

	@Column(name = "description")
	private String description;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false, columnDefinition = "issue_status")
	private IssueStatus status = IssueStatus.BACKLOG;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "priority", nullable = false, columnDefinition = "issue_priority")
	private IssuePriority priority = IssuePriority.MEDIUM;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "type", nullable = false, columnDefinition = "issue_type")
	private IssueType type = IssueType.TASK;

	@Column(name = "reporter_id", nullable = false)
	private UUID reporterId;

	@Column(name = "assignee_id")
	private UUID assigneeId;

	@Column(name = "parent_issue_id")
	private UUID parentIssueId;

	@Column(name = "story_points")
	private Integer storyPoints;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@Column(name = "resolved_at")
	private LocalDateTime resolvedAt;

	@Column(name = "closed_at")
	private LocalDateTime closedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version = 0L;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Setter(AccessLevel.NONE)
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
