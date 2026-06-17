package com.krish.issuetracker.exception;

import java.util.UUID;

public class LabelNotFoundException extends RuntimeException {

	public LabelNotFoundException(UUID labelId) {
		super("Label not found: " + labelId);
	}
}
