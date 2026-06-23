package com.kasibridge.fraud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends RuntimeException {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AlertNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Validation failed"
        );
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "An unexpected error occurred: " + ex.getMessage()));
    }

    @lombok.Data
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> fieldErrors;

        public static ErrorResponse of(int status, String message) {
            ErrorResponse r = new ErrorResponse();
            r.status = status;
            r.message = message;
            r.timestamp = LocalDateTime.now();
            return r;
        }
    }
}
