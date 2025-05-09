package org.example.warpol.exception;

public class UnauthorizedUnitAccessException extends RuntimeException {
    public UnauthorizedUnitAccessException(String message) {
        super(message);
    }
}
