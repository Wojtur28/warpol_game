package org.example.warpol.core.dto;

import org.example.warpol.core.entity.type.PlayerColor;

public record PlayerCommandRequest<T>(
        PlayerColor playerColor,
        T command
) {
}
