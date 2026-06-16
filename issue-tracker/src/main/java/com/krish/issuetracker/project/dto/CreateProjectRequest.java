package com.krish.issuetracker.project.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Size(min = 2, max = 10)
		@Pattern(regexp = "^[A-Z0-9]+$", message = "Key must be uppercase alphanumeric")
		String key,

		@NotNull
		UUID organizationId) {
}
