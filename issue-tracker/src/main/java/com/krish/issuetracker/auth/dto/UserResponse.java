package com.krish.issuetracker.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
		UUID id,
		String email,
		String fullName,
		boolean isActive,
		boolean emailVerified,
		LocalDateTime createdAt) {
}
