package org.example.warpol.core.repository;

import org.example.warpol.core.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    Optional<GameEntity> findByIsActiveTrue();
}
