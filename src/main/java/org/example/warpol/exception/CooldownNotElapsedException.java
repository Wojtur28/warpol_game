package org.example.warpol.exception;

public class CooldownNotElapsedException extends RuntimeException {
    public CooldownNotElapsedException(String message) {
        super(message);
    }
}
