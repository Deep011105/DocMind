package com.docmind.exception;

import com.docmind.dto.response.ApiResponse;
import com.docmind.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse buildError(String message) {
        return ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(
            MethodArgumentNotValidException e) {

        Map<String, String> errors = new HashMap<>();

        e.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Validation failed: {}", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(errorResponse));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<?>> handleBadCredentials(
            BadCredentialsException e) {

        log.warn("Invalid login attempt");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(
                        buildError("Invalid email or password")));
    }

    @ExceptionHandler(UserEmailNotExistException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotFound(
            UserEmailNotExistException e) {

        log.warn("User not found: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(
                        buildError(e.getMessage())));
    }

    @ExceptionHandler(UserEmailAlreadyExistException.class)
    public ResponseEntity<ApiResponse<?>> handleUserAlreadyExists(
            UserEmailAlreadyExistException e) {

        log.warn("User already exists: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(
                        buildError(e.getMessage())));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(
            ResourceNotFoundException e) {

        log.warn("Resource not found: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(
                        buildError(e.getMessage())));
    }

    @ExceptionHandler(NoTextExtractedException.class)
    public ResponseEntity<ApiResponse<?>> handleNoTextExtracted(
            NoTextExtractedException e) {

        log.warn("No text extracted: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.failure(
                        buildError(e.getMessage())));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e) {

        log.warn("File upload size exceeded");

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.failure(
                        buildError("File size exceeds allowed limit")));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(
            IllegalArgumentException e) {

        log.warn("Invalid request: {}", e.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(
                        buildError(e.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(
            AccessDeniedException e) {

        log.warn("Access denied");

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(
                        buildError("Access denied")));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(
            AuthenticationException e) {

        log.warn("Authentication failed");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(
                        buildError("Authentication failed")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(
            Exception e) {

        log.error("Unexpected error", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        buildError("Something went wrong")));
    }
}