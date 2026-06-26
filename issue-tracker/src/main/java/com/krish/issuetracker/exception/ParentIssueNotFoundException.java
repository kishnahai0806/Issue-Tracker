package com.krish.issuetracker.exception;

public class ParentIssueNotFoundException extends RuntimeException {

	public ParentIssueNotFoundException() {
		super("Parent issue not found in this project");
	}
}
