package com.krish.issuetracker.organization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationResponse(
		UUID id,
		String name,
		String slug,
		String plan,
		boolean isActive,
		LocalDateTime createdAt) {
}
