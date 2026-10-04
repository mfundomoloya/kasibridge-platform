package com.kasibridge.trader_profile.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //404 not found error
    @ExceptionHandler(TraderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TraderNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    //400 structured business validation errors
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(BusinessValidationException ex){
        ErrorResponse response = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage());

        response.setFieldErrors(ex.getFieldErrors());
        return ResponseEntity.badRequest().body(response);
    }

    //500 internal server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage()));
    }


    //409 conflict
    @ExceptionHandler(DuplicateTraderException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateTraderException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    //400 validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex){
        Map<String, String> fieldErrors = new HashMap<>();
        for(FieldError error : ex.getBindingResult().getFieldErrors()){
          fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Validation failed"
        );
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    //400 illegal entry
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    //error response shape
    @lombok.Data
    public static class ErrorResponse{
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> fieldErrors;

        public static ErrorResponse of(int status, String message){
            ErrorResponse r = new ErrorResponse();
            r.status = status;
            r.message = message;
            r.timestamp = LocalDateTime.now();
            return r;
        }
    }
}
