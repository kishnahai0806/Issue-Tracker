package com.krish.issuetracker.organization.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
		@Size(max = 100)
		String name) {
}
