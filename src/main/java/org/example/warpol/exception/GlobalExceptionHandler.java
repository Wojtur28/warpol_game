package org.example.warpol.exception;

import jakarta.persistence.OptimisticLockException;
import org.example.warpol.core.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CooldownNotElapsedException.class)
    public ResponseEntity<ErrorResponse> handleCooldownException(CooldownNotElapsedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_EARLY)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedUnitAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedUnitAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGameNotFound(GameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleOtherErrors(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidUnitCommandException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUnitCommand(InvalidUnitCommandException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getClass().getSimpleName(), "Another command has modified this unit. Try again."));
    }

}
