package com.petbuddy.user_profile_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

record ErrorResponse(
    String message,
    int status,
    String error,
    LocalDateTime timestamp,
    Map<String, String> details
) {}

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return createErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            new HashMap<>()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return createErrorResponse(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            errors
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            Map.of("error", ex.getMessage())
        );
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status, String message, Map<String, String> details) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            message,
            status.value(),
            status.getReasonPhrase(),
            LocalDateTime.now(),
            details
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}