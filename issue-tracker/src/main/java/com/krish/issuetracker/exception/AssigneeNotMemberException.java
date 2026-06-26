package com.krish.issuetracker.exception;

public class AssigneeNotMemberException extends RuntimeException {

	public AssigneeNotMemberException() {
		super("Assignee is not a member of this organization");
	}
}
