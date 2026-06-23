package com.krish.issuetracker.storage.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttachmentResponse(
		UUID id,
		UUID issueId,
		String fileName,
		long fileSizeBytes,
		String contentType,
		UUID uploadedBy,
		LocalDateTime createdAt) {
}
