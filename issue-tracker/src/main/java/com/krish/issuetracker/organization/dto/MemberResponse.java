package com.krish.issuetracker.organization.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.krish.issuetracker.domain.enums.UserRole;

public record MemberResponse(
		UUID userId,
		String fullName,
		String email,
		UserRole role,
		LocalDateTime joinedAt) {
}
