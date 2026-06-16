package com.krish.issuetracker.project.dto;

import java.util.UUID;

public record ProjectSummaryResponse(
		UUID id,
		UUID organizationId,
		String name,
		String key,
		boolean isArchived) {
}
