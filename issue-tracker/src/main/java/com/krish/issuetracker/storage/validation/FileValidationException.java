package com.krish.issuetracker.storage.validation;

public class FileValidationException extends RuntimeException {

	private final ValidationFailureReason reason;

	public FileValidationException(String message, ValidationFailureReason reason) {
		super(message);
		this.reason = reason;
	}

	public ValidationFailureReason getReason() {
		return reason;
	}
}
