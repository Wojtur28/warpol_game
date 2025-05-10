package org.example.warpol.core.service;

import lombok.RequiredArgsConstructor;
import org.example.warpol.core.config.GameConfig;
import org.example.warpol.core.dto.CommandResultResponse;
import org.example.warpol.core.entity.CommandEntity;
import org.example.warpol.core.repository.CommandRepository;
import org.example.warpol.core.repository.GameRepository;
import org.example.warpol.core.repository.UnitRepository;
import org.example.warpol.core.entity.GameEntity;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.type.UnitStatus;
import org.example.warpol.core.entity.type.UnitType;
import org.example.warpol.core.entity.unit.ArcherEntity;
import org.example.warpol.core.entity.unit.CannonEntity;
import org.example.warpol.core.entity.unit.TransportEntity;
import org.example.warpol.core.entity.unit.Unit;
import org.example.warpol.exception.CooldownNotElapsedException;
import org.example.warpol.exception.GameNotFoundException;
import org.example.warpol.exception.UnauthorizedUnitAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GameService {

    private final UnitRepository unitRepository;
    private final GameRepository gameRepository;
    private final CommandRepository commandRepository;
    private final GameConfig gameConfig;

    @Transactional
    public void createNewGameFromConfig() {
        Map<UnitType, Integer> unitsConfig = Map.of(
                UnitType.ARCHER, gameConfig.getUnits().getArcher(),
                UnitType.CANNON, gameConfig.getUnits().getCannon(),
                UnitType.TRANSPORT, gameConfig.getUnits().getTransport()
        );

        createNewGame(unitsConfig, gameConfig.getBoard().getWidth(), gameConfig.getBoard().getHeight());
    }

    @Transactional
    public void createNewGame(Map<UnitType, Integer> unitsConfig, int boardWidth, int boardHeight) {
        gameRepository.findAll().forEach(g -> {
            g.setActive(false);
            gameRepository.save(g);
        });
        GameEntity game = new GameEntity();
        game.setWidth(boardWidth);
        game.setHeight(boardHeight);
        game.setActive(true);
        game = gameRepository.save(game);

        Random random = new Random();
        Set<String> occupiedPositions = new HashSet<>();

        for (PlayerColor color : PlayerColor.values()) {
            for (Map.Entry<UnitType, Integer> entry : unitsConfig.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    Unit unit = switch (entry.getKey()) {
                        case ARCHER -> new ArcherEntity();
                        case TRANSPORT -> new TransportEntity();
                        case CANNON -> new CannonEntity();
                    };
                    unit.setColor(color);
                    unit.setType(entry.getKey());
                    unit.setStatus(UnitStatus.ACTIVE);

                    int x, y;
                    String key;
                    do {
                        x = random.nextInt(boardWidth);
                        y = random.nextInt(boardHeight);
                        key = x + ":" + y;
                    } while (occupiedPositions.contains(key));

                    occupiedPositions.add(key);
                    unit.setPositionX(x);
                    unit.setPositionY(y);
                    unit.setGame(game);
                    unitRepository.save(unit);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Unit> getUnits(PlayerColor color) {
        GameEntity game = gameRepository.findByIsActiveTrue()
                .orElseThrow(() -> new GameNotFoundException("No active game found"));

        return unitRepository.findByGameIdAndColor(game.getId(), color);
    }

    @Transactional
    public CommandResultResponse executeCommand(UUID unitId, CommandType commandType, int targetX, int targetY, PlayerColor playerColor) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        if (!unit.getColor().equals(playerColor)) {
            throw new UnauthorizedUnitAccessException("Unauthorized access to unit");
        }

        if (!unit.isCooldownElapsed(commandType)) {
            throw new CooldownNotElapsedException("Cooldown not elapsed for " + commandType);
        }

        List<Unit> unitsAtTarget = unitRepository.findAllByGameIdAndPositionXAndPositionY(
                unit.getGame().getId(), targetX, targetY);

        boolean isValidAction = isIsValidAction(commandType, targetX, targetY, unit);

        if (!isValidAction) {
            throw new RuntimeException("Invalid command for unit type");
        }

        boolean hasFriendlyUnit = false;
        switch (commandType) {
            case MOVE -> {
                hasFriendlyUnit = unitsAtTarget.stream()
                        .anyMatch(u -> u.getColor() == unit.getColor());

                if (hasFriendlyUnit) {
                    unit.setLastCommandTime(LocalDateTime.now());
                    unitRepository.save(unit);
                    break;
                }

                unitsAtTarget.stream()
                        .filter(u -> u.getColor() != unit.getColor())
                        .forEach(u -> {
                            u.destroy();
                            unitRepository.save(u);
                        });

                unit.move(targetX, targetY);
            }
            case SHOOT -> unitsAtTarget.forEach(target -> {
                target.destroy();
                unitRepository.save(target);
            });
        }

        CommandEntity command = new CommandEntity();
        command.setCommandType(commandType);
        command.setColor(playerColor);
        command.setTargetX(targetX);
        command.setTargetY(targetY);
        command.setExecutionTime(LocalDateTime.now());
        command.setGame(unit.getGame());
        command.setUnit(unit);
        commandRepository.save(command);

        unit.setLastCommandTime(LocalDateTime.now());
        unitRepository.save(unit);

        return new CommandResultResponse(
                "Unit moved to (" + targetX + "," + targetY + ")",
                !unitsAtTarget.isEmpty() && commandType == CommandType.SHOOT,
                commandType == CommandType.MOVE && !hasFriendlyUnit,
                commandType == CommandType.SHOOT
        );

    }

    private static boolean isIsValidAction(CommandType commandType, int targetX, int targetY, Unit unit) {
        return switch (unit.getType()) {
            case TRANSPORT -> commandType == CommandType.MOVE &&
                    (Math.abs(unit.getPositionX() - targetX) + Math.abs(unit.getPositionY() - targetY)) <= 3 &&
                    (unit.getPositionX() == targetX || unit.getPositionY() == targetY);

            case ARCHER -> commandType == CommandType.MOVE
                    ? Math.abs(unit.getPositionX() - targetX) + Math.abs(unit.getPositionY() - targetY) == 1
                    && (unit.getPositionX() == targetX || unit.getPositionY() == targetY)
                    : commandType == CommandType.SHOOT
                    && (unit.getPositionX() == targetX || unit.getPositionY() == targetY);

            case CANNON -> commandType == CommandType.SHOOT;
        };
    }

    @Transactional
    public CommandResultResponse executeRandomCommand(PlayerColor playerColor, UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        Random random = new Random();
        CommandType commandType = unit.getType() == UnitType.TRANSPORT ? CommandType.MOVE :
                random.nextBoolean() ? CommandType.MOVE : CommandType.SHOOT;

        int offsetX = random.nextInt(7) - 3;
        int offsetY = random.nextInt(7) - 3;
        int targetX = Math.min(Math.max(unit.getPositionX() + offsetX, 0), unit.getGame().getWidth() - 1);
        int targetY = Math.min(Math.max(unit.getPositionY() + offsetY, 0), unit.getGame().getHeight() - 1);

        return executeCommand(unitId, commandType, targetX, targetY, playerColor);
    }
}
