package com.wipuchi.logitrack.dto;

import com.wipuchi.logitrack.domain.Movement;
import com.wipuchi.logitrack.domain.MovementKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovementResponse(Long id, long warehouseId, long productId, MovementKind kind,
                               int quantity, BigDecimal unitCost, Instant occurredAt, UUID eventId, String epc) {
    public static MovementResponse from(Movement movement) {
        return new MovementResponse(movement.getId(), movement.getWarehouseId(), movement.getProductId(),
                movement.getKind(), movement.getQuantity(), movement.getUnitCost(), movement.getOccurredAt(),
                movement.getEventId(), movement.getEpc());
    }
}
