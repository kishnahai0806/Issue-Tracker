package com.krish.issuetracker.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(
		@NotBlank
		@Size(max = 100)
		String name,

		@NotBlank
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Must be a valid hex color e.g. #FF5733")
		String colorHex) {
}
