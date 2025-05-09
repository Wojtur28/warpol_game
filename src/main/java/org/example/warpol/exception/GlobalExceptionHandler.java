package org.example.warpol.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CooldownNotElapsedException.class)
    public ResponseEntity<String> handleCooldownException(CooldownNotElapsedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_EARLY).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedUnitAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccess(UnauthorizedUnitAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<String> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleOtherErrors(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
