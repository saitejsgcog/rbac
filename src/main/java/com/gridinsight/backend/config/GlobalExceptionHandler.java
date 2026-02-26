package com.gridinsight.backend.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;

import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_FAILED", errors));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(new ApiError("CONFLICT", List.of(ex.getMessage())));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(404).body(new ApiError("NOT_FOUND", List.of(ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body(new ApiError("INTERNAL_ERROR", List.of("Unexpected error")));
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(401).body(new ApiError("UNAUTHORIZED", List.of("Invalid email or password")));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLockedAccount(LockedException ex) {
        return ResponseEntity.status(403).body(new ApiError("ACCOUNT_LOCKED", List.of(ex.getMessage())));
    }


    record ApiError(String code, List<String> messages) {}
}