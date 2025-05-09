package org.example.warpol.core.dto;

import org.example.warpol.core.entity.type.CommandType;

import java.util.UUID;

public record ExecuteCommandRequest(
        UUID unitId,
        CommandType commandType,
        int targetX,
        int targetY
) {}
