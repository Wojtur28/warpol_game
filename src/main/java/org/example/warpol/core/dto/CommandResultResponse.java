package org.example.warpol.core.dto;

public record CommandResultResponse(
    String result,
    boolean unitDestroyed,
    boolean moved,
    boolean shotExecuted
) {}
