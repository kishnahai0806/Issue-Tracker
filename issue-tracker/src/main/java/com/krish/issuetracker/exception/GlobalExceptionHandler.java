package com.krish.issuetracker.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import com.krish.issuetracker.auth.EmailAlreadyExistsException;
import com.krish.issuetracker.auth.InvalidCredentialsException;
import com.krish.issuetracker.auth.InvalidRefreshTokenException;
import com.krish.issuetracker.auth.UserDisabledException;
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
		return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
			InvalidRefreshTokenException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}

	@ExceptionHandler(UserDisabledException.class)
	public ResponseEntity<ErrorResponse> handleUserDisabled(
			UserDisabledException ex,
			HttpServletRequest request) {
		return errorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
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

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleSpringSecurityAccessDenied(HttpServletRequest request) {
		return errorResponse(HttpStatus.FORBIDDEN, "Access denied", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception for request {}", request.getRequestURI(), ex);
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
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
