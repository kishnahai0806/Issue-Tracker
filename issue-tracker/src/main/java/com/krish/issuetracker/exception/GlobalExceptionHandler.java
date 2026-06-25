package com.krish.issuetracker.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import com.krish.issuetracker.auth.EmailAlreadyExistsException;
import com.krish.issuetracker.auth.InvalidCredentialsException;
import com.krish.issuetracker.auth.InvalidRefreshTokenException;
import com.krish.issuetracker.security.AuthFailureReason;
import com.krish.issuetracker.security.session.TokenHashingException;
import com.krish.issuetracker.storage.validation.FileValidationException;
import com.krish.issuetracker.storage.validation.ValidationFailureReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	private final MeterRegistry meterRegistry;

	public GlobalExceptionHandler(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
			EmailAlreadyExistsException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(
			InvalidCredentialsException ex,
			HttpServletRequest request) {
		incrementAuthFailure(AuthFailureReason.BAD_CREDENTIALS);
		return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
			InvalidRefreshTokenException ex,
			HttpServletRequest request) {
		incrementAuthFailure(AuthFailureReason.TOKEN_INVALID);
		return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationFailure(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining(", "));

		return errorResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMalformedRequestBody(HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, "Malformed or missing request body", request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(HttpServletRequest request) {
		return errorResponse(
				HttpStatus.CONFLICT,
				"Resource was modified by another request, please retry",
				request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(OrganizationNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleOrganizationNotFound(
			OrganizationNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(ProjectNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleProjectNotFound(
			ProjectNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(IssueNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleIssueNotFound(
			IssueNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCommentNotFound(
			CommentNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(LabelNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleLabelNotFound(
			LabelNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(AttachmentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleAttachmentNotFound(
			AttachmentNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(
			UserNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(MemberAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleMemberAlreadyExists(
			MemberAlreadyExistsException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(MemberNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleMemberNotFound(
			MemberNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler(OrganizationSlugAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleOrganizationSlugAlreadyExists(
			OrganizationSlugAlreadyExistsException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(LastOrganizationAdminException.class)
	public ResponseEntity<ErrorResponse> handleLastOrganizationAdmin(
			LastOrganizationAdminException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidDateRangeException.class)
	public ResponseEntity<ErrorResponse> handleInvalidDateRange(
			InvalidDateRangeException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleBusinessAccessDenied(HttpServletRequest request) {
		return errorResponse(HttpStatus.FORBIDDEN, "Access denied", request);
	}

	@ExceptionHandler(ProjectKeyAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleProjectKeyAlreadyExists(
			ProjectKeyAlreadyExistsException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(LabelAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleLabelAlreadyExists(
			LabelAlreadyExistsException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}

	@ExceptionHandler(AssigneeNotMemberException.class)
	public ResponseEntity<ErrorResponse> handleAssigneeNotMember(
			AssigneeNotMemberException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(LabelNotInProjectException.class)
	public ResponseEntity<ErrorResponse> handleLabelNotInProject(
			LabelNotInProjectException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(WatcherNotMemberException.class)
	public ResponseEntity<ErrorResponse> handleWatcherNotMember(
			WatcherNotMemberException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(ParentIssueNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleParentIssueNotFound(
			ParentIssueNotFoundException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(IssueNumberGenerationException.class)
	public ResponseEntity<ErrorResponse> handleIssueNumberGeneration(
			IssueNumberGenerationException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate issue number, please retry", request);
	}

	@ExceptionHandler(OptimisticLockException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(
			OptimisticLockException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.CONFLICT, "Issue was modified by another request, please retry", request);
	}

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleSpringSecurityAccessDenied(HttpServletRequest request) {
		return errorResponse(HttpStatus.FORBIDDEN, "Access denied", request);
	}

	@ExceptionHandler(FileValidationException.class)
	public ResponseEntity<ErrorResponse> handleFileValidation(
			FileValidationException ex,
			HttpServletRequest request) {
		ValidationFailureReason reason = ex.getReason();
		Counter.builder("file.upload.validation.failure")
				.tag("reason", reason.name())
				.register(meterRegistry)
				.increment();

		return switch (reason) {
			case SIZE_EXCEEDED -> errorResponse(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), request);
			case INVALID_TYPE -> errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request);
			case MAGIC_BYTE_MISMATCH -> errorResponse(
					HttpStatus.UNSUPPORTED_MEDIA_TYPE,
					"File content does not match declared type",
					request);
		};
	}

	@ExceptionHandler(StorageException.class)
	public ResponseEntity<ErrorResponse> handleStorageException(HttpServletRequest request) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Storage operation failed", request);
	}

	@ExceptionHandler(TokenHashingException.class)
	public ResponseEntity<ErrorResponse> handleTokenHashingException(HttpServletRequest request) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Token processing failed", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception for request {}", request.getRequestURI(), ex);
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	private void incrementAuthFailure(AuthFailureReason reason) {
		meterRegistry.counter(AuthFailureReason.METRIC_NAME, AuthFailureReason.REASON_TAG, reason.name()).increment();
	}

	private ResponseEntity<ErrorResponse> errorResponse(
			HttpStatus status,
			String message,
			HttpServletRequest request) {
		return ResponseEntity
				.status(status)
				.body(new ErrorResponse(
						Instant.now(),
						status.value(),
						status.getReasonPhrase(),
						message,
						request.getRequestURI()));
	}

	public record ErrorResponse(
			Instant timestamp,
			int status,
			String error,
			String message,
			String path) {
	}
}
