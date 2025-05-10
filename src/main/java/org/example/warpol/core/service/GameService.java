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
    private final Random random;
    private final RandomCommandHelper commandHelper;

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
                    } while (!occupiedPositions.add(key));

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
    public CommandResultResponse executeCommand(UUID unitId, CommandType commandType,
                                                int targetX, int targetY,
                                                PlayerColor playerColor) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        if (!unit.getColor().equals(playerColor)) {
            throw new UnauthorizedUnitAccessException("Unauthorized access to unit");
        }

        if (!unit.isCooldownElapsed(commandType)) {
            throw new CooldownNotElapsedException("Cooldown not elapsed for " + commandType);
        }

        List<Unit> atTarget = unitRepository.findAllByGameIdAndPositionXAndPositionY(
                unit.getGame().getId(), targetX, targetY);

        boolean valid = commandHelper.isValid(unit, commandType, targetX, targetY);

        if (!valid) {
            throw new RuntimeException("Invalid command for unit type");
        }

        boolean moved = false, shot = false, destroyed = false;

        switch (commandType) {
            case MOVE -> {
                boolean friendly = commandHelper.hasFriendly(unit, atTarget);
                if (friendly) {
                    unit.setLastCommandTime(LocalDateTime.now());
                    unitRepository.save(unit);
                    break;
                }
                destroyed = commandHelper.destroyEnemies(atTarget, unitRepository);
                unit.move(targetX, targetY);
                moved = true;
            }
            case SHOOT -> {
                destroyed = commandHelper.destroyEnemies(atTarget, unitRepository);
                shot = true;
            }
        }

        CommandEntity cmd = commandHelper.buildCommand(unit, commandType, targetX, targetY);
        commandRepository.save(cmd);

        unit.setLastCommandTime(LocalDateTime.now());
        unitRepository.save(unit);

        String res = commandType == CommandType.SHOOT
                ? "Unit shot to (" + targetX + "," + targetY + ")"
                : moved ? "Unit moved to (" + targetX + "," + targetY + ")"
                : "Unit stayed in place";
        return new CommandResultResponse(res, destroyed, moved, shot);
    }

    @Transactional
    public CommandResultResponse executeRandomCommand(PlayerColor playerColor, UUID unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        CommandType type = commandHelper.getRandomCommandForUnit(unit, random);

        int[] target = commandHelper.getRandomTargetFor(unit, type, unit.getGame().getWidth(), unit.getGame().getHeight(), random);

        return executeCommand(unitId, type, target[0], target[1], playerColor);
    }
}
