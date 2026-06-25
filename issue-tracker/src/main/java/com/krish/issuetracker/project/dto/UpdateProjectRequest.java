package com.krish.issuetracker.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
		@Size(max = 100)
		String name,
		String description) {
}
