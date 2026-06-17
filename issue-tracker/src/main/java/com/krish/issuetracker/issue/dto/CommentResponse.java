package com.krish.issuetracker.issue.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
		UUID id,
		UUID issueId,
		UUID authorId,
		String content,
		boolean isEdited,
		LocalDateTime editedAt,
		LocalDateTime createdAt) {
}
