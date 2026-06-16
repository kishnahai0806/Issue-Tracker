package com.krish.issuetracker.issue.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
		UUID id,
		UUID issueId,
		UUID changedBy,
		String fieldName,
		String oldValue,
		String newValue,
		LocalDateTime changedAt) {
}
