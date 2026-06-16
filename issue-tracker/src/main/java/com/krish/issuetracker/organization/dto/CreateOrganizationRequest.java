package com.krish.issuetracker.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Size(max = 50)
		@Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase alphanumeric with hyphens")
		String slug) {
}
