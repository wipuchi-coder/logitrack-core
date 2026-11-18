package com.wipuchi.logitrack.dto;

import com.wipuchi.logitrack.domain.Warehouse;
import jakarta.validation.constraints.*;

public final class WarehouseDto {
    private WarehouseDto() {}

    public record Input(@NotBlank @Size(max = 32) String code,
                        @NotBlank @Size(max = 120) String name) {
        public Warehouse toEntity() {
            return new Warehouse(code.strip(), name.strip());
        }
    }

    public record Output(Long id, String code, String name) {
        public static Output from(Warehouse entity) {
            return new Output(entity.getId(), entity.getCode(), entity.getName());
        }
    }
}
