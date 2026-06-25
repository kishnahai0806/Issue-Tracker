package com.krish.issuetracker.project.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
		UUID id,
		UUID organizationId,
		String name,
		String key,
		String description,
		UUID createdBy,
		boolean isArchived,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
