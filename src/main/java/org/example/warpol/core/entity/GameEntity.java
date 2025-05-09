package org.example.warpol.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.warpol.core.entity.unit.Unit;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class GameEntity extends BaseEntity {

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Unit> units = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommandEntity> commands = new ArrayList<>();

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private boolean isActive;
}
