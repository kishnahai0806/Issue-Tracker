package com.krish.issuetracker.domain.enums;

public enum IssuePriority {
	LOWEST("LOWEST"),
	LOW("LOW"),
	MEDIUM("MEDIUM"),
	HIGH("HIGH"),
	HIGHEST("HIGHEST"),
	CRITICAL("CRITICAL");

	private final String dbValue;

	IssuePriority(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}
}
