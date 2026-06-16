package com.krish.issuetracker.exception;

import java.util.UUID;

public class IssueNotFoundException extends RuntimeException {

	public IssueNotFoundException(UUID issueId) {
		super("Issue not found: " + issueId);
	}
}
