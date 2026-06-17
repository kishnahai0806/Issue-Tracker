package com.krish.issuetracker.label.dto;

import java.util.UUID;

public record LabelResponse(
		UUID id,
		UUID projectId,
		String name,
		String colorHex) {
}
