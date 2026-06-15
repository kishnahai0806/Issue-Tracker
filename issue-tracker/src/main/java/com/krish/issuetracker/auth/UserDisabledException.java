package com.krish.issuetracker.auth;

import java.util.UUID;

public class UserDisabledException extends RuntimeException {

	public UserDisabledException(UUID userId) {
		super("User account is disabled: " + userId);
	}
}
