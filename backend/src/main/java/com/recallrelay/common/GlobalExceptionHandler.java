package com.recallrelay.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        var firstError = exception.getBindingResult().getFieldErrors().stream().findFirst();
        if (firstError.isPresent()) {
            var error = firstError.get();
            return ResponseEntity.badRequest().body(
                    ErrorResponse.field("VALIDATION_ERROR", error.getDefaultMessage(), error.getField())
            );
        }
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("VALIDATION_ERROR", "Request validation failed.")
        );
    }
}
