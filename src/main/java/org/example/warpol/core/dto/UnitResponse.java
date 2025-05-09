package org.example.warpol.core.dto;

import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.type.UnitStatus;
import org.example.warpol.core.entity.type.UnitType;
import org.example.warpol.core.entity.unit.Unit;

import java.util.UUID;

public record UnitResponse(
        UUID id,
        UnitType type,
        PlayerColor color,
        UnitStatus status,
        int positionX,
        int positionY,
        int commandCount
) {
    public static UnitResponse from(Unit unit) {
        return new UnitResponse(
                unit.getId(),
                unit.getType(),
                unit.getColor(),
                unit.getStatus(),
                unit.getPositionX(),
                unit.getPositionY(),
                unit.getCommandCount()
        );
    }
}

