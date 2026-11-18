package com.wipuchi.logitrack.repository;

import com.wipuchi.logitrack.domain.Movement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MovementRepository extends JpaRepository<Movement, Long> {
    Optional<Movement> findByEventId(UUID eventId);
}
