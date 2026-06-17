package com.krish.issuetracker.storage.validation;

public class FileValidationException extends RuntimeException {

	private final FileTypeValidator.ValidationFailureReason reason;

	public FileValidationException(String message, FileTypeValidator.ValidationFailureReason reason) {
		super(message);
		this.reason = reason;
	}

	public Enum<?> getReason() {
		return reason;
	}
}
