package com.krish.issuetracker.domain.enums;

public enum IssueType {
	BUG("BUG"),
	FEATURE("FEATURE"),
	TASK("TASK"),
	IMPROVEMENT("IMPROVEMENT"),
	EPIC("EPIC"),
	SUBTASK("SUBTASK");

	private final String dbValue;

	IssueType(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}
}
