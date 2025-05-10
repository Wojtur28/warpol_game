package org.example.warpol.core.repository;

import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.unit.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByGameIdAndColor(UUID gameId, PlayerColor color);

    List<Unit> findAllByGameIdAndPositionXAndPositionY(UUID id, int targetX, int targetY);
}
