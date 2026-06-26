package com.krish.issuetracker.security.session;

public class TokenHashingException extends IllegalStateException {

	public TokenHashingException(String message, Throwable cause) {
		super(message, cause);
	}
}
