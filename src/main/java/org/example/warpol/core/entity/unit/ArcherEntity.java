package org.example.warpol.core.entity.unit;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.warpol.core.entity.type.CommandType;

import java.time.Duration;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class ArcherEntity extends Unit {

    @Override
    public Duration getCooldown(CommandType commandType) {
        return switch (commandType) {
            case MOVE, RANDOM_MOVE -> Duration.ofSeconds(5);
            case SHOOT -> Duration.ofSeconds(10);
        };
    }
}
