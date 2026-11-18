package com.wipuchi.logitrack;

import com.wipuchi.logitrack.domain.*;
import com.wipuchi.logitrack.dto.MovementRequest;
import com.wipuchi.logitrack.exception.BusinessException;
import com.wipuchi.logitrack.repository.*;
import com.wipuchi.logitrack.service.MovementService;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {
    @Mock MovementRepository movements;
    @Mock WarehouseRepository warehouses;
    @Mock ProductRepository products;
    @Mock JdbcTemplate jdbc;
    @InjectMocks MovementService service;

    @Test
    void snapshotsCostOnNewMovement() {
        mockCatalog();
        when(movements.saveAndFlush(any(Movement.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.record(new MovementRequest(1, 2, MovementKind.IN, 3, null, null));
        assertTrue(result.created());
        assertEquals(new BigDecimal("4.50"), result.movement().unitCost());
        assertEquals(3, result.movement().quantity());
    }

    @Test
    void handlesInsufficientStock() {
        mockCatalog();
        when(movements.saveAndFlush(any(Movement.class))).thenThrow(
                new DataIntegrityViolationException("constraint", new SQLException("Saldo insuficiente", "LT001")));
        var exception = assertThrows(BusinessException.class,
                () -> service.record(new MovementRequest(1, 2, MovementKind.OUT, 3, null, null)));
        assertEquals(HttpStatus.CONFLICT, exception.status());
        assertEquals("Saldo insuficiente", exception.getMessage());
    }

    @Test
    void replaysMatchingRfidEventWithoutWriting() {
        UUID event = UUID.randomUUID();
        mockExistingEvent(event);
        var result = service.record(new MovementRequest(1, 2, MovementKind.OUT, 3, event, "E200"));
        assertFalse(result.created());
        assertEquals(event, result.movement().eventId());
        verify(movements, never()).saveAndFlush(any());
        verifyNoInteractions(warehouses, products);
    }

    @Test
    void rejectsRfidEventWithDifferentPayload() {
        UUID event = UUID.randomUUID();
        mockExistingEvent(event);
        var exception = assertThrows(BusinessException.class,
                () -> service.record(new MovementRequest(1, 2, MovementKind.OUT, 4, event, "E200")));
        assertEquals(HttpStatus.CONFLICT, exception.status());
        verify(movements, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnknownWarehouse() {
        var exception = assertThrows(BusinessException.class,
                () -> service.record(new MovementRequest(9, 2, MovementKind.IN, 3, null, null)));
        assertEquals(HttpStatus.NOT_FOUND, exception.status());
        verifyNoInteractions(movements, products);
    }

    private void mockCatalog() {
        when(warehouses.existsById(1L)).thenReturn(true);
        when(products.findById(2L)).thenReturn(Optional.of(new Product("SKU", "Caja", new BigDecimal("4.50"))));
    }

    private void mockExistingEvent(UUID event) {
        when(movements.findByEventId(event)).thenReturn(Optional.of(
                new Movement(1, 2, MovementKind.OUT, 3, BigDecimal.TEN, Instant.now(), event, "E200")));
    }
}
