package com.wipuchi.logitrack.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String name;
    private BigDecimal unitCost;

    protected Product() {}

    public Product(String sku, String name, BigDecimal unitCost) {
        this.sku = sku;
        this.name = name;
        this.unitCost = unitCost;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getUnitCost() { return unitCost; }
}
