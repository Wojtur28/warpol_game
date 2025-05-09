package org.example.warpol.core.service;

import lombok.RequiredArgsConstructor;
import org.example.warpol.core.config.GameConfig;
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
        Map<UnitType, Integer> unitMap = Map.of(
                UnitType.ARCHER, gameConfig.getUnits().getArcher(),
                UnitType.CANNON, gameConfig.getUnits().getCannon(),
                UnitType.TRANSPORT, gameConfig.getUnits().getTransport()
        );

        createNewGame(unitMap, gameConfig.getBoard().getWidth(), gameConfig.getBoard().getHeight());
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
                    unit.setPositionX(random.nextInt(boardWidth));
                    unit.setPositionY(random.nextInt(boardHeight));
                    unit.setGame(game);
                    unitRepository.save(unit);
                }
            }
        }
    }

    public List<Unit> getUnits(PlayerColor color) {
        GameEntity game = gameRepository.findByIsActiveTrue()
                .orElseThrow(() -> new GameNotFoundException("No active game found"));

        return unitRepository.findByGameIdAndColor(game.getId(), color);
    }

    @Transactional
    public void executeCommand(UUID unitId, CommandType commandType, int targetX, int targetY, PlayerColor playerColor) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        if (!unit.getColor().equals(playerColor)) {
            throw new UnauthorizedUnitAccessException("Unauthorized access to unit");
        }

        if (!unit.isCooldownElapsed(commandType)) {
            throw new CooldownNotElapsedException("Cooldown not elapsed for " + commandType);
        }

        Optional<Unit> targetUnitOpt = unitRepository.findByGameIdAndPositionXAndPositionY(
                unit.getGame().getId(), targetX, targetY);

        boolean isValidAction = switch (unit.getType()) {
            case TRANSPORT -> commandType == CommandType.MOVE &&
                    (Math.abs(unit.getPositionX() - targetX) + Math.abs(unit.getPositionY() - targetY)) <= 3 &&
                    (unit.getPositionX() == targetX || unit.getPositionY() == targetY);

            case ARCHER -> switch (commandType) {
                case MOVE -> Math.abs(unit.getPositionX() - targetX) + Math.abs(unit.getPositionY() - targetY) == 1
                        && (unit.getPositionX() == targetX || unit.getPositionY() == targetY);
                case SHOOT -> (unit.getPositionX() == targetX || unit.getPositionY() == targetY);
                default -> false;
            };

            case CANNON -> commandType == CommandType.SHOOT &&
                    Math.abs(unit.getPositionX() - targetX) == Math.abs(unit.getPositionY() - targetY);
        };

        if (!isValidAction) {
            throw new RuntimeException("Invalid command for unit type");
        }

        switch (commandType) {
            case MOVE -> {
                if (targetUnitOpt.isPresent()) {
                    Unit targetUnit = targetUnitOpt.get();
                    if (targetUnit.getColor() == unit.getColor()) {
                        unit.setLastCommandTime(LocalDateTime.now());
                        unitRepository.save(unit);
                        break;
                    } else {
                        targetUnit.destroy();
                        unitRepository.save(targetUnit);
                    }
                }
                unit.move(targetX, targetY);
            }
            case SHOOT -> targetUnitOpt.ifPresent(target -> {
                target.destroy();
                unitRepository.save(target);
            });
        }

        CommandEntity command = new CommandEntity();
        command.setCommandType(commandType);
        command.setColor(playerColor);
        command.setTargetX(targetX);
        command.setTargetY(targetY);
        command.setExecuted(true);
        command.setExecutionTime(LocalDateTime.now());
        command.setGame(unit.getGame());
        command.setUnit(unit);
        commandRepository.save(command);

        unit.setLastCommandTime(LocalDateTime.now());
        unitRepository.save(unit);
    }



    @Transactional
    public void executeRandomCommand(PlayerColor playerColor, UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        Random random = new Random();
        CommandType commandType = unit.getType() == UnitType.TRANSPORT ? CommandType.MOVE :
                random.nextBoolean() ? CommandType.MOVE : CommandType.SHOOT;

        int targetX = Math.min(Math.max(unit.getPositionX() + random.nextInt(7) - 3, 0), unit.getGame().getWidth() - 1);
        int targetY = Math.min(Math.max(unit.getPositionY() + random.nextInt(7) - 3, 0), unit.getGame().getHeight() - 1);

        executeCommand(unitId, commandType, targetX, targetY, playerColor);
    }
}
