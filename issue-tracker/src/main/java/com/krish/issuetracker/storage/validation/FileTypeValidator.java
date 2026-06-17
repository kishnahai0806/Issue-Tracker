package com.krish.issuetracker.storage.validation;

import java.io.IOException;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class FileTypeValidator {

	// Explicitly rejected content types — no magic byte
	// validation is possible for these types:
	// text/plain   — text/html is indistinguishable
	// text/html    — executes in browsers, XSS vector
	// text/javascript — executes in browsers, XSS vector
	private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"application/pdf",
			"image/png",
			"image/jpeg",
			"image/gif");

	public void validate(MultipartFile file, long maxFileSizeBytes) {
		if (file.getSize() > maxFileSizeBytes) {
			throw new FileValidationException(
					"File exceeds maximum size of " + maxFileSizeBytes + " bytes",
					ValidationFailureReason.SIZE_EXCEEDED);
		}

		String contentType = file.getContentType();
		if (!ALLOWED_TYPES.contains(contentType)) {
			throw new FileValidationException(
					"File type not allowed: " + contentType,
					ValidationFailureReason.INVALID_TYPE);
		}

		byte[] bytes = fileBytes(file);
		if (bytes.length < 4 || !magicBytesMatch(contentType, bytes)) {
			throw new FileValidationException(
					"File content does not match declared type",
					ValidationFailureReason.MAGIC_BYTE_MISMATCH);
		}
	}

	private byte[] fileBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException ex) {
			log.debug("File bytes could not be read for validation", ex);
			throw new FileValidationException(
					"File content does not match declared type",
					ValidationFailureReason.MAGIC_BYTE_MISMATCH);
		}
	}

	private boolean magicBytesMatch(String contentType, byte[] bytes) {
		return switch (contentType) {
			case "application/pdf" -> startsWith(bytes, PDF_MAGIC);
			case "image/png" -> startsWith(bytes, PNG_MAGIC);
			case "image/jpeg" -> startsWith(bytes, JPEG_MAGIC);
			case "image/gif" -> startsWith(bytes, GIF_MAGIC);
			default -> false;
		};
	}

	private boolean startsWith(byte[] fileBytes, byte[] magic) {
		if (fileBytes.length < magic.length) {
			return false;
		}

		for (int i = 0; i < magic.length; i++) {
			if (fileBytes[i] != magic[i]) {
				return false;
			}
		}

		return true;
	}

	enum ValidationFailureReason {
		SIZE_EXCEEDED,
		INVALID_TYPE,
		MAGIC_BYTE_MISMATCH
	}
}
