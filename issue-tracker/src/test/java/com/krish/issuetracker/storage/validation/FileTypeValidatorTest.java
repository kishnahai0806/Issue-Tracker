package com.krish.issuetracker.storage.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

	private static final long MAX_FILE_SIZE_BYTES = 10485760L;

	private final FileTypeValidator validator = new FileTypeValidator();

	@Test
	void validate_shouldPassForValidPdfFile() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.pdf",
				"application/pdf",
				new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

		assertThatNoException().isThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES));
	}

	@Test
	void validate_shouldPassForValidPngFile() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.png",
				"image/png",
				new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x00});

		assertThatNoException().isThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES));
	}

	@Test
	void validate_shouldPassForValidJpegFile() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.jpg",
				"image/jpeg",
				new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});

		assertThatNoException().isThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES));
	}

	@Test
	void validate_shouldThrowSizeExceededWhenFileTooLarge() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.pdf",
				"application/pdf",
				new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

		assertThatThrownBy(() -> validator.validate(file, 4L))
				.isInstanceOf(FileValidationException.class)
				.extracting(ex -> ((FileValidationException) ex).getReason())
				.isEqualTo(ValidationFailureReason.SIZE_EXCEEDED);
	}

	@Test
	void validate_shouldThrowInvalidTypeForDisallowedContentType() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.txt",
				"text/plain",
				"hello".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES))
				.isInstanceOf(FileValidationException.class)
				.extracting(ex -> ((FileValidationException) ex).getReason())
				.isEqualTo(ValidationFailureReason.INVALID_TYPE);
	}

	@Test
	void validate_shouldThrowMagicByteMismatchWhenBytesDoNotMatch() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.png",
				"image/png",
				new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

		assertThatThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES))
				.isInstanceOf(FileValidationException.class)
				.extracting(ex -> ((FileValidationException) ex).getReason())
				.isEqualTo(ValidationFailureReason.MAGIC_BYTE_MISMATCH);
	}

	@Test
	void validate_shouldThrowMagicByteMismatchForHtmlDisguisedAsPdf() {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"test.pdf",
				"application/pdf",
				"<html>".getBytes(StandardCharsets.UTF_8));

		assertThatThrownBy(() -> validator.validate(file, MAX_FILE_SIZE_BYTES))
				.isInstanceOf(FileValidationException.class)
				.extracting(ex -> ((FileValidationException) ex).getReason())
				.isEqualTo(ValidationFailureReason.MAGIC_BYTE_MISMATCH);
	}
}
