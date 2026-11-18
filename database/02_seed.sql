BEGIN;
INSERT INTO warehouses (code, name) VALUES
    ('BOG-01', 'Bogotá Centro'), ('MDE-01', 'Medellín Norte');
INSERT INTO products (sku, name, unit_cost) VALUES
    ('EMB-001', 'Caja corrugada mediana', 1.80),
    ('EMB-002', 'Rollo de cinta industrial', 4.50),
    ('SEG-001', 'Guantes de nitrilo', 3.25),
    ('SEG-002', 'Gafas de protección', 8.90),
    ('RFD-001', 'Etiqueta RFID UHF', 0.65),
    ('RFD-002', 'Lector RFID portátil', 320.00);

INSERT INTO movements (warehouse_id, product_id, kind, quantity, unit_cost, occurred_at)
SELECT w.id, p.id, 'IN', 50 + (7 - p.id) * 20, p.unit_cost, now() - interval '90 days'
FROM warehouses w CROSS JOIN products p;

-- Cuatro movimientos diarios y reposición semanal según la rotación de cada SKU.
INSERT INTO movements (warehouse_id, product_id, kind, quantity, unit_cost, occurred_at)
SELECT w.id, p.id, CASE WHEN d % 7 = 0 THEN 'IN' ELSE 'OUT' END,
       CASE WHEN d % 7 = 0 THEN (7 - p.id) * 8 ELSE 1 + (d + slot + w.id) % (7 - p.id) END,
       p.unit_cost, now() - interval '90 days' + d * interval '1 day' + slot * interval '2 hours'
FROM generate_series(0, 89) d
CROSS JOIN generate_series(1, 4) slot
JOIN warehouses w ON w.id = 1 + (d + slot) % 2
JOIN products p ON p.id = 1 + (d + 2 * slot) % 6
ORDER BY d, slot;
COMMIT;
