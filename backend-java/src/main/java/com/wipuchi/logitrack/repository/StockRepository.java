package com.wipuchi.logitrack.repository;

import com.wipuchi.logitrack.dto.StockResponse;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StockRepository {
    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StockResponse> findByWarehouse(long warehouseId, long afterProductId, int limit) {
        return jdbc.query("""
                SELECT p.id, p.sku, i.quantity
                FROM inventory i JOIN products p ON p.id = i.product_id
                WHERE i.warehouse_id = ? AND i.product_id > ?
                ORDER BY i.product_id
                LIMIT ?
                """, (row, index) -> new StockResponse(
                        row.getLong("id"), row.getString("sku"), row.getLong("quantity")),
                warehouseId, afterProductId, limit);
    }
}
