package org.example.warpol.core.entity.unit;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.warpol.core.entity.BaseEntity;
import org.example.warpol.core.entity.GameEntity;
import org.example.warpol.core.entity.type.CommandType;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.type.UnitStatus;
import org.example.warpol.core.entity.type.UnitType;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Unit extends BaseEntity {

    @Version
    private Integer version;

    private int positionX;
    private int positionY;

    private int commandCount;

    @Enumerated(EnumType.STRING)
    private PlayerColor color;

    @Enumerated(EnumType.STRING)
    private UnitStatus status;

    @Enumerated(EnumType.STRING)
    private UnitType type;

    private LocalDateTime lastCommandTime;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private GameEntity game;

    public abstract Duration getCooldown(CommandType commandType);

    public boolean isCooldownElapsed(CommandType commandType) {
        if (lastCommandTime == null) return true;
        Duration cooldown = getCooldown(commandType);
        return LocalDateTime.now().isAfter(lastCommandTime.plus(cooldown));
    }

    public void move(int newX, int newY) {
        this.positionX = newX;
        this.positionY = newY;
        this.commandCount++;
        this.lastCommandTime = LocalDateTime.now();
    }

    public void destroy() {
        this.status = UnitStatus.DESTROYED;
    }
}
