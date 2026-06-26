package com.krish.issuetracker.exception;

public class LabelNotInProjectException extends RuntimeException {

	public LabelNotInProjectException() {
		super("One or more labels do not belong to this project");
	}
}
