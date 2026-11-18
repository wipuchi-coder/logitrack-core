package com.wipuchi.logitrack.controller;

import com.wipuchi.logitrack.dto.ProductDto.*;
import com.wipuchi.logitrack.repository.ProductRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<Output> list(@PageableDefault(sort = "id") Pageable page) {
        return repository.findAll(page).map(Output::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Output> get(@PathVariable long id) {
        return ResponseEntity.of(repository.findById(id).map(Output::from));
    }

    @PostMapping
    public ResponseEntity<Output> create(@Valid @RequestBody Input input) {
        var result = Output.from(repository.save(input.toEntity()));
        return ResponseEntity.created(URI.create("/api/products/" + result.id())).body(result);
    }
}
