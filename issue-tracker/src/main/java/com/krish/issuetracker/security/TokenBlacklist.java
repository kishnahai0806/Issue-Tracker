package com.krish.issuetracker.security;

public interface TokenBlacklist {

	void blacklist(String token, long expiryMs);

	boolean isBlacklisted(String token);
}
