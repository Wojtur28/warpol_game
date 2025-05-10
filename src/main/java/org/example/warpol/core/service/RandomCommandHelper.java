package org.example.warpol.core.service;

import org.example.warpol.core.entity.CommandEntity;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.unit.Unit;
import org.example.warpol.core.repository.UnitRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class RandomCommandHelper {

    public CommandType getRandomCommandForUnit(Unit unit, Random random) {
        List<CommandType> possible = new ArrayList<>();
        switch (unit.getType()) {
            case TRANSPORT -> possible.add(CommandType.MOVE);
            case ARCHER -> {
                possible.add(CommandType.MOVE);
                possible.add(CommandType.SHOOT);
            }
            case CANNON -> possible.add(CommandType.SHOOT);
        }
        return possible.get(random.nextInt(possible.size()));
    }

    public int[] getRandomTargetFor(Unit unit, CommandType type,
                                    int boardWidth, int boardHeight,
                                    Random random) {
        int x = unit.getPositionX();
        int y = unit.getPositionY();
        switch (unit.getType()) {
            case ARCHER -> {
                int distance = type == CommandType.MOVE ? 1
                        : random.nextInt(Math.max(boardWidth, boardHeight) - 1) + 1;
                if (random.nextBoolean()) {
                    x += random.nextBoolean() ? distance : -distance;
                } else {
                    y += random.nextBoolean() ? distance : -distance;
                }
            }
            case TRANSPORT -> {
                int distance = random.nextInt(3) + 1;
                if (random.nextBoolean()) {
                    x += random.nextBoolean() ? distance : -distance;
                } else {
                    y += random.nextBoolean() ? distance : -distance;
                }
            }
            case CANNON -> {
                int dist = random.nextInt(Math.max(boardWidth, boardHeight) - 1) + 1;
                if (random.nextBoolean()) {
                    if (random.nextBoolean()) x += random.nextBoolean() ? dist : -dist;
                    else y += random.nextBoolean() ? dist : -dist;
                } else {
                    int d2 = random.nextInt(Math.max(boardWidth, boardHeight) - 1) + 1;
                    x += random.nextBoolean() ? d2 : -d2;
                    y += random.nextBoolean() ? d2 : -d2;
                }
            }
        }
        x = clamp(x, 0, boardWidth - 1);
        y = clamp(y, 0, boardHeight - 1);
        return new int[]{x, y};
    }

    public boolean isValid(Unit unit, CommandType commandType,
                           int targetX, int targetY) {
        return switch (unit.getType()) {
            case TRANSPORT -> commandType == CommandType.MOVE
                    && (Math.abs(unit.getPositionX() - targetX)
                    + Math.abs(unit.getPositionY() - targetY)) <= 3
                    && (unit.getPositionX() == targetX
                    || unit.getPositionY() == targetY);
            case ARCHER -> commandType == CommandType.MOVE
                    ? Math.abs(unit.getPositionX() - targetX)
                    + Math.abs(unit.getPositionY() - targetY) == 1
                    && (unit.getPositionX() == targetX
                    || unit.getPositionY() == targetY)
                    : commandType == CommandType.SHOOT
                    && (unit.getPositionX() == targetX
                    || unit.getPositionY() == targetY);
            case CANNON -> commandType == CommandType.SHOOT;
        };
    }

    public boolean hasFriendly(Unit unit, List<Unit> unitsAtTarget) {
        return unitsAtTarget.stream()
                .anyMatch(u -> u.getColor() == unit.getColor());
    }

    public boolean destroyEnemies(List<Unit> unitsAtTarget, UnitRepository repo) {
        if (unitsAtTarget.isEmpty()) {
            return false;
        }
        for (Unit u : unitsAtTarget) {
            u.destroy();
            repo.save(u);
        }
        return true;
    }

    public CommandEntity buildCommand(Unit unit, CommandType commandType,
                                      int targetX, int targetY) {
        CommandEntity cmd = new CommandEntity();
        cmd.setCommandType(commandType);
        cmd.setColor(unit.getColor());
        cmd.setTargetX(targetX);
        cmd.setTargetY(targetY);
        cmd.setExecutionTime(LocalDateTime.now());
        cmd.setGame(unit.getGame());
        cmd.setUnit(unit);
        return cmd;
    }

    private int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
