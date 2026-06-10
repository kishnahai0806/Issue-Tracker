package com.krish.issuetracker.domain.entity;

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
@Table(name = "issue_audit_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IssueAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(name = "issue_id", nullable = false)
	private UUID issueId;

	@Column(name = "changed_by", nullable = false)
	private UUID changedBy;

	@Column(name = "field_name", nullable = false, length = 100)
	private String fieldName;

	@Column(name = "old_value")
	private String oldValue;

	@Column(name = "new_value")
	private String newValue;

	@Setter(AccessLevel.NONE)
	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	@PrePersist
	void onCreate() {
		if (changedAt == null) {
			changedAt = LocalDateTime.now();
		}
	}
}
