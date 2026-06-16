package com.krish.issuetracker.exception;

import java.util.UUID;

public class LabelAlreadyExistsException extends RuntimeException {

	public LabelAlreadyExistsException(String name, UUID projectId) {
		super("Label '" + name + "' already exists in project: " + projectId);
	}
}
