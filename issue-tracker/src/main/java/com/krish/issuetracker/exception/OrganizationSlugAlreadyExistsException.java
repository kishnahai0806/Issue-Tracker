package com.krish.issuetracker.exception;

public class OrganizationSlugAlreadyExistsException extends RuntimeException {

	public OrganizationSlugAlreadyExistsException(String slug) {
		super("Organization slug already taken: " + slug);
	}
}
