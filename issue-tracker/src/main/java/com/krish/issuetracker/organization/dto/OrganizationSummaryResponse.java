package com.krish.issuetracker.organization.dto;

import java.util.UUID;

public record OrganizationSummaryResponse(
		UUID id,
		String name,
		String slug,
		String plan) {
}
