package org.example.warpol.exception;

public class InvalidUnitCommandException extends RuntimeException {
    public InvalidUnitCommandException(String message) {
        super(message);
    }
}
