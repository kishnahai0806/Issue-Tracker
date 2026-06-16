package com.krish.issuetracker.exception;

import java.util.UUID;

public class MemberAlreadyExistsException extends RuntimeException {

	public MemberAlreadyExistsException(UUID userId, UUID organizationId) {
		super("User " + userId + " is already a member of organization " + organizationId);
	}
}
