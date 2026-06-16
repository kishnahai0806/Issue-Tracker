package com.krish.issuetracker.exception;

import java.util.UUID;

public class MemberNotFoundException extends RuntimeException {

	public MemberNotFoundException(UUID userId, UUID organizationId) {
		super("User " + userId + " is not a member of organization " + organizationId);
	}
}
