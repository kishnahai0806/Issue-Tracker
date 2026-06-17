package com.krish.issuetracker.exception;

import java.util.UUID;

public class IssueNumberGenerationException extends RuntimeException {

	public IssueNumberGenerationException(UUID projectId) {
		super("Failed to generate issue number for project: " + projectId);
	}
}
