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
@Table(name = "issue_attachments")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IssueAttachment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	@EqualsAndHashCode.Include
	private UUID id;

	@Column(name = "issue_id", nullable = false)
	private UUID issueId;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_size_bytes", nullable = false)
	private Long fileSizeBytes;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Column(name = "uploaded_by", nullable = false)
	private UUID uploadedBy;

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
