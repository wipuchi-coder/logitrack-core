package com.wipuchi.logitrack.service;

import com.wipuchi.logitrack.domain.Movement;
import com.wipuchi.logitrack.dto.MovementRequest;
import com.wipuchi.logitrack.dto.MovementResponse;
import com.wipuchi.logitrack.exception.BusinessException;
import com.wipuchi.logitrack.repository.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class MovementService {
    private final MovementRepository movements;
    private final WarehouseRepository warehouses;
    private final ProductRepository products;
    private final JdbcTemplate jdbc;

    public MovementService(MovementRepository movements, WarehouseRepository warehouses,
                           ProductRepository products, JdbcTemplate jdbc) {
        this.movements = movements;
        this.warehouses = warehouses;
        this.products = products;
        this.jdbc = jdbc;
    }

    public record Result(MovementResponse movement, boolean created) {}

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Result record(MovementRequest request) {
        if (request.eventId() != null) {
            jdbc.queryForObject("SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(?, 0))",
                    Integer.class, request.eventId().toString());
            var existing = movements.findByEventId(request.eventId());
            if (existing.isPresent()) {
                Movement movement = existing.get();
                if (movement.getWarehouseId() != request.warehouseId()
                        || movement.getProductId() != request.productId()
                        || movement.getQuantity() != request.quantity()
                        || movement.getKind() != request.kind()
                        || !Objects.equals(movement.getEpc(), request.epc())) {
                    throw new BusinessException(HttpStatus.CONFLICT, "El UUID RFID ya tiene otro contenido");
                }
                return new Result(MovementResponse.from(movement), false);
            }
        }

        if (!warehouses.existsById(request.warehouseId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Almacén inexistente");
        }
        var product = products.findById(request.productId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Producto inexistente"));
        var movement = new Movement(request.warehouseId(), request.productId(), request.kind(),
                request.quantity(), product.getUnitCost(), Instant.now().truncatedTo(ChronoUnit.MICROS),
                request.eventId(), request.epc());
        try {
            return new Result(MovementResponse.from(movements.saveAndFlush(movement)), true);
        } catch (DataAccessException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof SQLException sql && "LT001".equals(sql.getSQLState())) {
                    throw new BusinessException(HttpStatus.CONFLICT, "Saldo insuficiente");
                }
            }
            throw exception;
        }
    }
}
