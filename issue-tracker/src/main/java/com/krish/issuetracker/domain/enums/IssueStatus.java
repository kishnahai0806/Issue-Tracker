package com.krish.issuetracker.domain.enums;

public enum IssueStatus {
	BACKLOG("BACKLOG"),
	TODO("TODO"),
	IN_PROGRESS("IN_PROGRESS"),
	IN_REVIEW("IN_REVIEW"),
	DONE("DONE"),
	CLOSED("CLOSED"),
	CANCELLED("CANCELLED");

	private final String dbValue;

	IssueStatus(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}
}
