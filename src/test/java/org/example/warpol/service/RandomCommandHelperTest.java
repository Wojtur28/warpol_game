package org.example.warpol.service;

import org.example.warpol.core.entity.GameEntity;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.type.UnitType;
import org.example.warpol.core.entity.unit.ArcherEntity;
import org.example.warpol.core.entity.unit.CannonEntity;
import org.example.warpol.core.entity.unit.TransportEntity;
import org.example.warpol.core.entity.unit.Unit;
import org.example.warpol.core.repository.UnitRepository;
import org.example.warpol.core.service.RandomCommandHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RandomCommandHelperTest {

    private RandomCommandHelper helper;
    private Random random;

    @BeforeEach
    void setUp() {
        random = mock(Random.class);
        helper = new RandomCommandHelper();
    }

    @Test
    void getRandomCommand_transportAlwaysMove() {
        TransportEntity unit = new TransportEntity();
        unit.setType(UnitType.TRANSPORT);
        doReturn(0).when(random).nextInt(anyInt());

        CommandType cmd = helper.getRandomCommandForUnit(unit, random);
        assertThat(cmd).isEqualTo(CommandType.MOVE);
    }

    @Test
    void getRandomCommand_archerReturnsMoveOrShoot() {
        ArcherEntity unit = new ArcherEntity();
        unit.setType(UnitType.ARCHER);
        doReturn(0).when(random).nextInt(2);
        assertThat(helper.getRandomCommandForUnit(unit, random))
                .isEqualTo(CommandType.MOVE);
        doReturn(1).when(random).nextInt(2);
        assertThat(helper.getRandomCommandForUnit(unit, random))
                .isEqualTo(CommandType.SHOOT);
    }

    @Test
    void getRandomCommand_cannonAlwaysShoot() {
        CannonEntity unit = new CannonEntity();
        unit.setType(UnitType.CANNON);
        doReturn(0).when(random).nextInt(anyInt());
        assertThat(helper.getRandomCommandForUnit(unit, random))
                .isEqualTo(CommandType.SHOOT);
    }

    @Test
    void isValid_transportValidAndInvalid() {
        TransportEntity unit = new TransportEntity();
        unit.setType(UnitType.TRANSPORT);
        unit.setPositionX(5);
        unit.setPositionY(5);
        assertThat(helper.isValid(unit, CommandType.MOVE, 8, 5)).isTrue();
        assertThat(helper.isValid(unit, CommandType.MOVE, 5, 9)).isFalse();
        assertThat(helper.isValid(unit, CommandType.SHOOT, 6, 5)).isFalse();
    }

    @Test
    void isValid_archerValidAndInvalid() {
        ArcherEntity unit = new ArcherEntity();
        unit.setType(UnitType.ARCHER);
        unit.setPositionX(2);
        unit.setPositionY(2);
        assertThat(helper.isValid(unit, CommandType.MOVE, 3, 2)).isTrue();
        assertThat(helper.isValid(unit, CommandType.MOVE, 3, 3)).isFalse();
        assertThat(helper.isValid(unit, CommandType.SHOOT, 2, 5)).isTrue();
        assertThat(helper.isValid(unit, CommandType.SHOOT, 3, 3)).isFalse();
    }

    @Test
    void isValid_cannonAlwaysShootAllowed() {
        CannonEntity unit = new CannonEntity();
        unit.setType(UnitType.CANNON);
        unit.setPositionX(1);
        unit.setPositionY(1);
        assertThat(helper.isValid(unit, CommandType.SHOOT, 4, 4)).isTrue();
        assertThat(helper.isValid(unit, CommandType.SHOOT, 1, 5)).isTrue();
        assertThat(helper.isValid(unit, CommandType.MOVE, 2, 1)).isFalse();
    }

    @Test
    void getRandomTarget_transportWithinBounds() {
        TransportEntity unit = new TransportEntity();
        unit.setType(UnitType.TRANSPORT);
        unit.setPositionX(2);
        unit.setPositionY(2);
        doReturn(2).when(random).nextInt(3);
        doReturn(true).when(random).nextBoolean();
        doReturn(true).when(random).nextBoolean();
        int[] target = helper.getRandomTargetFor(unit, CommandType.MOVE, 5, 5, random);
        assertThat(target[0]).isBetween(0, 4);
        assertThat(target[1]).isBetween(0, 4);
    }

    @Test
    void destroyEnemies_emptyListReturnsFalse() {
        UnitRepository repo = mock(UnitRepository.class);
        boolean res = helper.destroyEnemies(Collections.emptyList(), repo);
        assertThat(res).isFalse();
        verify(repo, never()).save(any());
    }

    @Test
    void destroyEnemies_nonEmptyDestroysAll() {
        Unit u1 = mock(Unit.class);
        Unit u2 = mock(Unit.class);
        UnitRepository repo = mock(UnitRepository.class);
        boolean res = helper.destroyEnemies(List.of(u1, u2), repo);
        assertThat(res).isTrue();
        verify(u1).destroy();
        verify(u2).destroy();
        verify(repo).save(u1);
        verify(repo).save(u2);
    }

    @Test
    void buildCommand_populatesFields() {
        TransportEntity unit = new TransportEntity();
        unit.setType(UnitType.TRANSPORT);
        unit.setPositionX(1);
        unit.setPositionY(2);
        GameEntity game = new GameEntity();
        unit.setGame(game);
        var cmd = helper.buildCommand(unit, CommandType.MOVE, 3, 4);
        assertThat(cmd.getCommandType()).isEqualTo(CommandType.MOVE);
        assertThat(cmd.getTargetX()).isEqualTo(3);
        assertThat(cmd.getTargetY()).isEqualTo(4);
        assertThat(cmd.getUnit()).isSameAs(unit);
        assertThat(cmd.getGame()).isSameAs(game);
        assertThat(cmd.getExecutionTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void hasFriendly_detectsFriendlyAndEnemy() {
        TransportEntity unit = new TransportEntity();
        unit.setType(UnitType.TRANSPORT);
        unit.setColor(PlayerColor.WHITE);
        Unit friendly = new TransportEntity();
        friendly.setType(UnitType.TRANSPORT);
        friendly.setColor(PlayerColor.WHITE);
        Unit enemy = new TransportEntity();
        enemy.setType(UnitType.TRANSPORT);
        enemy.setColor(PlayerColor.BLACK);
        assertThat(helper.hasFriendly(unit, List.of(enemy))).isFalse();
        assertThat(helper.hasFriendly(unit, List.of(enemy, friendly))).isTrue();
    }
}
