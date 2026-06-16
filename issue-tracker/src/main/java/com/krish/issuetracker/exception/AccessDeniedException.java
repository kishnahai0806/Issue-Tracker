package com.krish.issuetracker.exception;

public class AccessDeniedException extends RuntimeException {

	public AccessDeniedException() {
		super("Access denied");
	}
}
