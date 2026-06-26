package com.krish.issuetracker.exception;

public class WatcherNotMemberException extends RuntimeException {

	public WatcherNotMemberException() {
		super("User is not a member of this organization");
	}
}
