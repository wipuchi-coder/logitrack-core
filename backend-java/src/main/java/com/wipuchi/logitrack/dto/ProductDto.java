package com.wipuchi.logitrack.dto;

import com.wipuchi.logitrack.domain.Product;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class ProductDto {
    private ProductDto() {}

    public record Input(@NotBlank @Size(max = 48) String sku,
                        @NotBlank @Size(max = 120) String name,
                        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal unitCost) {
        public Product toEntity() {
            return new Product(sku.strip(), name.strip(), unitCost);
        }
    }

    public record Output(Long id, String sku, String name, BigDecimal unitCost) {
        public static Output from(Product entity) {
            return new Output(entity.getId(), entity.getSku(), entity.getName(), entity.getUnitCost());
        }
    }
}
