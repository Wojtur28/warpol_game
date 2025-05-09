package org.example.warpol.core.dto;

import org.example.warpol.core.entity.type.UnitType;

import java.util.Map;

public record NewGameRequest(
        Map<UnitType, Integer> units,
        int width,
        int height
) {}
