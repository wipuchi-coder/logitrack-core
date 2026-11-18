package com.wipuchi.logitrack.dto;

import com.wipuchi.logitrack.domain.MovementKind;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record MovementRequest(
        @Positive long warehouseId,
        @Positive long productId,
        @NotNull MovementKind kind,
        @Positive int quantity,
        UUID eventId,
        @Pattern(regexp = ".*\\S.*") @Size(max = 64) String epc
) {
    @AssertTrue(message = "eventId y epc deben enviarse juntos")
    public boolean isRfidMetadataComplete() {
        return (eventId == null) == (epc == null);
    }
}
