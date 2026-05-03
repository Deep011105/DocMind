package com.docMind.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success;      // Always false for errors
    private final String message;       // Error message
    private final String errorCode;     // Custom error code (optional)
    private final Object details;       // Extra details (validation errors, etc.)
    private final LocalDateTime timestamp;

}