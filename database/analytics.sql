-- 1. ABC global por valor de consumo. El SKU que cruza un umbral conserva la clase anterior.
WITH consumption AS (
    SELECT product_id, sum(quantity * unit_cost) AS value
    FROM movements WHERE kind = 'OUT' AND occurred_at > now() - interval '90 days'
    GROUP BY product_id
), ranked AS (
    SELECT *, sum(value) OVER () AS total,
           sum(value) OVER (ORDER BY value DESC, product_id ROWS UNBOUNDED PRECEDING) - value AS prior
    FROM consumption
)
SELECT p.sku, r.value,
       CASE WHEN r.total = 0 THEN 'C' WHEN r.prior < r.total * 0.8 THEN 'A'
            WHEN r.prior < r.total * 0.95 THEN 'B' ELSE 'C' END AS abc
FROM ranked r JOIN products p ON p.id = r.product_id
ORDER BY r.value DESC, p.id;

-- 2. Demanda diaria y media móvil de siete días, incluidos los días sin salidas (UTC).
WITH days AS (
    SELECT (now() AT TIME ZONE 'UTC')::date - offset_days AS day
    FROM generate_series(0, 29) offset_days
), daily AS (
    SELECT (occurred_at AT TIME ZONE 'UTC')::date AS day, sum(quantity) AS units
    FROM movements WHERE kind = 'OUT' AND occurred_at >= now() - interval '30 days'
    GROUP BY 1
)
SELECT days.day, coalesce(units, 0) AS units,
       avg(coalesce(units, 0)) OVER (ORDER BY days.day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS avg_7
FROM days LEFT JOIN daily USING (day) ORDER BY days.day;

-- 3. Cobertura por almacén y producto con demanda media de 30 días.
WITH demand AS (
    SELECT warehouse_id, product_id, sum(quantity) / 30.0 AS daily_units
    FROM movements WHERE kind = 'OUT' AND occurred_at > now() - interval '30 days'
    GROUP BY warehouse_id, product_id
)
SELECT i.warehouse_id, i.product_id, i.quantity,
       round(i.quantity / nullif(d.daily_units, 0), 2) AS coverage_days
FROM inventory i LEFT JOIN demand d USING (warehouse_id, product_id)
ORDER BY coverage_days NULLS LAST, i.warehouse_id, i.product_id;

-- 4. Tres SKU con más unidades despachadas por almacén en 90 días.
WITH demand AS (
    SELECT warehouse_id, product_id, sum(quantity) AS units
    FROM movements WHERE kind = 'OUT' AND occurred_at > now() - interval '90 days'
    GROUP BY warehouse_id, product_id
), ranked AS (
    SELECT *, row_number() OVER (PARTITION BY warehouse_id ORDER BY units DESC, product_id) AS position
    FROM demand
)
SELECT * FROM ranked WHERE position <= 3 ORDER BY warehouse_id, position;

-- 5. Saldo histórico acumulado por almacén y SKU en orden de evento.
WITH signed AS (
    SELECT id, warehouse_id, product_id, occurred_at,
           CASE WHEN kind = 'IN' THEN quantity ELSE -quantity END AS delta
    FROM movements
)
SELECT *, sum(delta) OVER (
    PARTITION BY warehouse_id, product_id ORDER BY occurred_at, id ROWS UNBOUNDED PRECEDING
) AS balance
FROM signed ORDER BY warehouse_id, product_id, occurred_at, id;
