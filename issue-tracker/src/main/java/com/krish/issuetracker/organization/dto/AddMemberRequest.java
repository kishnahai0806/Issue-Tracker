package com.krish.issuetracker.organization.dto;

import java.util.UUID;

import com.krish.issuetracker.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
		@NotNull
		UUID userId,

		@NotNull
		UserRole role) {
}
