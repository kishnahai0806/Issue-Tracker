package com.krish.issuetracker.security.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

final class TokenHasher {

	private static final String HASH_ALGORITHM = "SHA-256";

	private TokenHasher() {
	}

	static String sha256Base64Url(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new TokenHashingException(HASH_ALGORITHM + " is not available", ex);
		}
	}
}
