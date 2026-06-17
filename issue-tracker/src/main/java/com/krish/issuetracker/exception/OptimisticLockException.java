package com.krish.issuetracker.exception;

public class OptimisticLockException extends RuntimeException {

	public OptimisticLockException() {
		super("Issue was modified by another request, please retry");
	}
}
