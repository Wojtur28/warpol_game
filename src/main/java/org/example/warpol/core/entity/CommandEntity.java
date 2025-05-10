package org.example.warpol.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.unit.Unit;

import java.time.LocalDateTime;

@Entity
@Table(name = "commands")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class CommandEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private CommandType commandType;

    @Enumerated(EnumType.STRING)
    private PlayerColor color;

    private int targetX;

    private int targetY;

    private LocalDateTime executionTime;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameEntity game;

    @ManyToOne
    @JoinColumn(name = "unit_id", nullable = false, updatable = false)
    private Unit unit;
}

