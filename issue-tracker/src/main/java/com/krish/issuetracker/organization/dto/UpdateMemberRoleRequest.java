package com.krish.issuetracker.organization.dto;

import com.krish.issuetracker.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
		@NotNull
		UserRole role) {
}
