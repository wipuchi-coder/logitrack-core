from decimal import Decimal
from typing import Literal

from pydantic import BaseModel


class ProductMetrics(BaseModel):
    product_id: int
    sku: str
    on_hand: int
    demand_units: int
    consumption_value: Decimal
    abc_class: Literal["A", "B", "C"]
    average_daily_units: Decimal
    coverage_days: Decimal | None
    reorder_point: int
    reorder_needed: bool


class WarehouseAnalytics(BaseModel):
    warehouse_id: int
    window_days: int
    lead_time_days: int
    safety_days: int
    items: list[ProductMetrics]
