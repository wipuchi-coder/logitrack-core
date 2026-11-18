package com.wipuchi.logitrack.controller;

import com.wipuchi.logitrack.dto.WarehouseDto.*;
import com.wipuchi.logitrack.dto.StockResponse;
import com.wipuchi.logitrack.exception.BusinessException;
import com.wipuchi.logitrack.repository.StockRepository;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import com.wipuchi.logitrack.repository.WarehouseRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses")
@SecurityRequirement(name = "bearerAuth")
public class WarehouseController {
    private final WarehouseRepository repository;

    private final StockRepository stock;

    public WarehouseController(WarehouseRepository repository, StockRepository stock) {
        this.repository = repository;
        this.stock = stock;
    }

    @GetMapping
    public Page<Output> list(@PageableDefault(sort = "id") Pageable page) {
        return repository.findAll(page).map(Output::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Output> get(@PathVariable long id) {
        return ResponseEntity.of(repository.findById(id).map(Output::from));
    }

    @GetMapping("/{id}/stock")
    public List<StockResponse> stock(@PathVariable @Positive long id,
                                     @RequestParam(defaultValue = "0") @PositiveOrZero long afterProductId,
                                     @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        if (!repository.existsById(id)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Almacén inexistente");
        }
        return stock.findByWarehouse(id, afterProductId, limit);
    }

    @PostMapping
    public ResponseEntity<Output> create(@Valid @RequestBody Input input) {
        var result = Output.from(repository.save(input.toEntity()));
        return ResponseEntity.created(URI.create("/api/warehouses/" + result.id())).body(result);
    }
}
