package com.krish.issuetracker.issue.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record WatcherRequest(
		@NotNull
		UUID userId) {
}
