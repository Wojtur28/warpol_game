package org.example.warpol.core.dto;

public record ErrorResponse(
    String error,
    String message
) {}
