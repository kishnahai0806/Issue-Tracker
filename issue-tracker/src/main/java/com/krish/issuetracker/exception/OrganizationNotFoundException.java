package com.krish.issuetracker.exception;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {

	public OrganizationNotFoundException(UUID organizationId) {
		super("Organization not found: " + organizationId);
	}
}
