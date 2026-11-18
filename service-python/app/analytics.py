from dataclasses import dataclass
from decimal import Decimal
from math import ceil

from sqlalchemy import text
from sqlalchemy.engine import Connection

from .schemas import ProductMetrics

DEMAND_QUERY = text("""
    WITH demand AS (
        SELECT product_id, sum(quantity) AS demand_units,
               sum(quantity * unit_cost) AS consumption_value
        FROM movements
        WHERE warehouse_id = :warehouse_id AND kind = 'OUT'
          AND occurred_at > now() - make_interval(days => :window_days)
          AND occurred_at <= now()
        GROUP BY product_id
    )
    SELECT p.id AS product_id, p.sku, i.quantity AS on_hand,
           coalesce(d.demand_units, 0) AS demand_units,
           coalesce(d.consumption_value, 0) AS consumption_value
    FROM inventory i JOIN products p ON p.id = i.product_id
    LEFT JOIN demand d ON d.product_id = i.product_id
    WHERE i.warehouse_id = :warehouse_id
""")


@dataclass(frozen=True)
class DemandRow:
    product_id: int
    sku: str
    on_hand: int
    demand_units: int
    consumption_value: Decimal


def load_demand(connection: Connection, warehouse_id: int, window_days: int) -> list[DemandRow]:
    rows = connection.execute(
        DEMAND_QUERY, {"warehouse_id": warehouse_id, "window_days": window_days}
    ).mappings()
    return [DemandRow(**row) for row in rows]


def calculate_metrics(
    rows: list[DemandRow], window_days: int, lead_time_days: int, safety_days: int
) -> list[ProductMetrics]:
    ranked = sorted(rows, key=lambda row: (-row.consumption_value, row.product_id))
    total_value = sum(row.consumption_value for row in ranked)
    cumulative_value = Decimal(0)
    metrics = []

    for row in ranked:
        if row.consumption_value <= 0:
            abc_class = "C"
        elif cumulative_value < total_value * Decimal("0.80"):
            abc_class = "A"
        elif cumulative_value < total_value * Decimal("0.95"):
            abc_class = "B"
        else:
            abc_class = "C"
        cumulative_value += row.consumption_value

        daily_units = Decimal(row.demand_units) / window_days
        coverage = Decimal(row.on_hand * window_days) / row.demand_units if row.demand_units else None
        reorder_point = ceil(Decimal(row.demand_units * (lead_time_days + safety_days)) / window_days)
        metrics.append(ProductMetrics(
            product_id=row.product_id,
            sku=row.sku,
            on_hand=row.on_hand,
            demand_units=row.demand_units,
            consumption_value=row.consumption_value,
            abc_class=abc_class,
            average_daily_units=round(daily_units, 4),
            coverage_days=round(coverage, 2) if coverage is not None else None,
            reorder_point=reorder_point,
            reorder_needed=row.demand_units > 0 and row.on_hand <= reorder_point,
        ))

    return metrics
