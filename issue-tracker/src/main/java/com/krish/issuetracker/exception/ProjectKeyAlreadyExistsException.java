package com.krish.issuetracker.exception;

import java.util.UUID;

public class ProjectKeyAlreadyExistsException extends RuntimeException {

	public ProjectKeyAlreadyExistsException(String key, UUID organizationId) {
		super("Project key " + key + " already exists in organization " + organizationId);
	}
}
