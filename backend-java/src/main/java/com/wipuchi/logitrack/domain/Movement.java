package com.wipuchi.logitrack.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movements")
public class Movement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private long warehouseId;
    private long productId;
    @Enumerated(EnumType.STRING)
    private MovementKind kind;
    private int quantity;
    private BigDecimal unitCost;
    private Instant occurredAt;
    private UUID eventId;
    private String epc;

    protected Movement() {}

    public Movement(long warehouseId, long productId, MovementKind kind, int quantity,
                    BigDecimal unitCost, Instant occurredAt, UUID eventId, String epc) {
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.kind = kind;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.occurredAt = occurredAt;
        this.eventId = eventId;
        this.epc = epc;
    }

    public Long getId() { return id; }
    public long getWarehouseId() { return warehouseId; }
    public long getProductId() { return productId; }
    public MovementKind getKind() { return kind; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getEventId() { return eventId; }
    public String getEpc() { return epc; }
}
