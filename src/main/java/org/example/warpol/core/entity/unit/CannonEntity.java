package org.example.warpol.core.entity.unit;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.warpol.core.entity.type.CommandType;

import java.time.Duration;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class CannonEntity extends Unit {

    @Override
    public Duration getCooldown(CommandType commandType) {
        return Duration.ofSeconds(13);
    }
}
