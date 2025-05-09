package org.example.warpol.core.repository;

import org.example.warpol.core.entity.CommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommandRepository extends JpaRepository<CommandEntity, UUID> {
}
