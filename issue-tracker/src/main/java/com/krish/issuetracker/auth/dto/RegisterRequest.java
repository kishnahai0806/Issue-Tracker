package com.krish.issuetracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank
		@Email
		String email,

		// BCrypt only uses the first 72 bytes, so validation caps password length.
		@NotBlank
		@Size(min = 8, max = 72)
		String password,

		@NotBlank
		@Size(max = 100)
		String fullName) {
}
