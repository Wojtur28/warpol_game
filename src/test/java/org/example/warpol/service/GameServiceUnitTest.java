package org.example.warpol.service;

import jakarta.persistence.OptimisticLockException;
import org.example.warpol.core.config.GameConfig;
import org.example.warpol.core.dto.CommandResultResponse;
import org.example.warpol.core.entity.GameEntity;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.unit.ArcherEntity;
import org.example.warpol.core.entity.unit.TransportEntity;
import org.example.warpol.core.entity.unit.Unit;
import org.example.warpol.core.repository.CommandRepository;
import org.example.warpol.core.repository.GameRepository;
import org.example.warpol.core.repository.UnitRepository;
import org.example.warpol.core.service.GameService;
import org.example.warpol.core.service.RandomCommandHelper;
import org.example.warpol.exception.CooldownNotElapsedException;
import org.example.warpol.exception.UnauthorizedUnitAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GameServiceUnitTest {
    private UnitRepository unitRepository;
    private GameRepository gameRepository;
    private CommandRepository commandRepository;
    private GameConfig gameConfig;
    private Random random;
    private RandomCommandHelper helper;
    private GameService service;

    @BeforeEach
    void init() {
        unitRepository = mock(UnitRepository.class);
        gameRepository = mock(GameRepository.class);
        commandRepository = mock(CommandRepository.class);
        gameConfig = new GameConfig();
        gameConfig.setBoard(new GameConfig.Board());
        gameConfig.getBoard().setWidth(5);
        gameConfig.getBoard().setHeight(5);
        gameConfig.setUnits(new GameConfig.Units());
        gameConfig.getUnits().setArcher(1);
        gameConfig.getUnits().setCannon(1);
        gameConfig.getUnits().setTransport(1);
        random = new Random(42);
        helper = mock(RandomCommandHelper.class);
        service = new GameService(unitRepository, gameRepository, commandRepository,
                gameConfig, random, helper);
    }

    @Test
    void createNewGameFromConfig_setsUnits() {
        when(gameRepository.findAll()).thenReturn(List.of(new GameEntity()));
        when(gameRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createNewGameFromConfig();

        verify(gameRepository, times(2)).save(any(GameEntity.class));
        int expectedUnits = (gameConfig.getUnits().getArcher()
                + gameConfig.getUnits().getTransport()
                + gameConfig.getUnits().getCannon())
                * PlayerColor.values().length;
        verify(unitRepository, times(expectedUnits)).save(any(Unit.class));
    }

    @Test
    void getUnits_returnsUnitsForActiveGame() {
        GameEntity active = new GameEntity();
        active.setId(UUID.randomUUID());
        active.setActive(true);
        List<Unit> list = List.of(mock(Unit.class));
        when(gameRepository.findByIsActiveTrue()).thenReturn(Optional.of(active));
        when(unitRepository.findByGameIdAndColor(active.getId(), PlayerColor.BLACK))
                .thenReturn(list);

        var result = service.getUnits(PlayerColor.BLACK);
        assertThat(result).isSameAs(list);
    }

    @Test
    void executeCommand_moveDestroysEnemy_andMoves() {
        TransportEntity unit = mock(TransportEntity.class);
        UUID id = UUID.randomUUID();
        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(unit.getColor()).thenReturn(PlayerColor.WHITE);
        when(unit.isCooldownElapsed(CommandType.MOVE)).thenReturn(true);
        GameEntity game = new GameEntity();
        game.setId(UUID.randomUUID());
        game.setWidth(5);
        game.setHeight(5);
        when(unit.getGame()).thenReturn(game);
        when(unitRepository.findAllByGameIdAndPositionXAndPositionY(game.getId(),3,2))
                .thenReturn(List.of(mock(Unit.class)));
        when(helper.isValid(unit, CommandType.MOVE,3,2)).thenReturn(true);
        when(helper.destroyEnemies(anyList(), eq(unitRepository))).thenReturn(true);

        CommandResultResponse resp = service.executeCommand(id, CommandType.MOVE,3,2,PlayerColor.WHITE);
        assertThat(resp.moved()).isTrue();
        assertThat(resp.unitDestroyed()).isTrue();
        assertThat(resp.result()).contains("moved to (3,2)");
        verify(unit).move(3,2);
    }

    @Test
    void executeCommand_shootDestroysAndResponds() {
        ArcherEntity unit = mock(ArcherEntity.class);
        UUID id = UUID.randomUUID();
        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(unit.getColor()).thenReturn(PlayerColor.BLACK);
        when(unit.isCooldownElapsed(CommandType.SHOOT)).thenReturn(true);
        GameEntity game = new GameEntity();
        game.setId(UUID.randomUUID());
        game.setWidth(5);
        game.setHeight(5);
        when(unit.getGame()).thenReturn(game);
        when(helper.isValid(unit, CommandType.SHOOT,1,2)).thenReturn(true);
        when(unitRepository.findAllByGameIdAndPositionXAndPositionY(game.getId(),1,2))
                .thenReturn(List.of(mock(Unit.class), mock(Unit.class)));
        when(helper.destroyEnemies(anyList(), eq(unitRepository))).thenReturn(true);

        CommandResultResponse resp = service.executeCommand(id, CommandType.SHOOT,1,2,PlayerColor.BLACK);
        assertThat(resp.shotExecuted()).isTrue();
        assertThat(resp.unitDestroyed()).isTrue();
        assertThat(resp.result()).contains("shot to (1,2)");
    }

    @Test
    void executeRandomCommand_usesHelper_andDelegates() {
        UUID id = UUID.randomUUID();
        TransportEntity unit = mock(TransportEntity.class);
        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(helper.getRandomCommandForUnit(unit, random)).thenReturn(CommandType.MOVE);
        when(helper.getRandomTargetFor(unit, CommandType.MOVE,5,5, random))
                .thenReturn(new int[]{2,3});
        when(helper.isValid(unit, CommandType.MOVE,2,3)).thenReturn(true);
        when(unit.isCooldownElapsed(CommandType.MOVE)).thenReturn(true);
        when(unit.getColor()).thenReturn(PlayerColor.WHITE);
        GameEntity game = new GameEntity();
        game.setId(UUID.randomUUID());
        game.setWidth(5);
        game.setHeight(5);
        when(unit.getGame()).thenReturn(game);
        when(unitRepository.findAllByGameIdAndPositionXAndPositionY(game.getId(),2,3))
                .thenReturn(Collections.emptyList());
        when(helper.destroyEnemies(anyList(), eq(unitRepository))).thenReturn(false);

        CommandResultResponse r = service.executeRandomCommand(PlayerColor.WHITE, id);
        assertThat(r.moved()).isTrue();
        assertThat(r.shotExecuted()).isFalse();
        assertThat(r.result()).contains("moved to (2,3)");
    }

    @Test
    void executeCommand_invalidColor_throws() {
        UUID id = UUID.randomUUID();
        Unit unit = mock(Unit.class);
        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(unit.getColor()).thenReturn(PlayerColor.WHITE);
        assertThatThrownBy(() -> service.executeCommand(id, CommandType.MOVE,0,0,PlayerColor.BLACK))
                .isInstanceOf(UnauthorizedUnitAccessException.class);
    }

    @Test
    void executeCommand_cooldownNotElapsed_throws() {
        UUID id = UUID.randomUUID();
        Unit unit = mock(Unit.class);
        when(unitRepository.findById(id)).thenReturn(Optional.of(unit));
        when(unit.getColor()).thenReturn(PlayerColor.WHITE);
        when(unit.isCooldownElapsed(CommandType.MOVE)).thenReturn(false);
        assertThatThrownBy(() -> service.executeCommand(id, CommandType.MOVE,0,0,PlayerColor.WHITE))
                .isInstanceOf(CooldownNotElapsedException.class);
    }

    @Test
    void optimisticLock_onFindById_throws() {
        UUID id = UUID.randomUUID();
        when(unitRepository.findById(id)).thenThrow(OptimisticLockException.class);
        assertThatThrownBy(() -> service.executeCommand(id, CommandType.MOVE,0,0,PlayerColor.WHITE))
                .isInstanceOf(OptimisticLockException.class);
    }
}

