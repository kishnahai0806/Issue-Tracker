package com.krish.issuetracker.domain.enums;

public enum UserRole {
	ADMIN("ADMIN"),
	PROJECT_MANAGER("PROJECT_MANAGER"),
	DEVELOPER("DEVELOPER"),
	REPORTER("REPORTER");

	private final String dbValue;

	UserRole(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}
}
