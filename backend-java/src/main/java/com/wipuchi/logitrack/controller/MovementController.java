package com.wipuchi.logitrack.controller;

import com.wipuchi.logitrack.dto.*;
import com.wipuchi.logitrack.repository.MovementRepository;
import com.wipuchi.logitrack.service.MovementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movements")
@SecurityRequirement(name = "bearerAuth")
public class MovementController {
    private final MovementService service;
    private final MovementRepository movements;

    public MovementController(MovementService service, MovementRepository movements) {
        this.service = service;
        this.movements = movements;
    }

    @GetMapping
    public Page<MovementResponse> list(@PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable page) {
        return movements.findAll(page).map(MovementResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovementResponse> get(@PathVariable long id) {
        return ResponseEntity.of(movements.findById(id).map(MovementResponse::from));
    }

    @PostMapping
    public ResponseEntity<MovementResponse> create(@Valid @RequestBody MovementRequest request) {
        var result = service.record(request);
        if (!result.created()) {
            return ResponseEntity.ok(result.movement());
        }
        return ResponseEntity.created(URI.create("/api/movements/" + result.movement().id())).body(result.movement());
    }
}
