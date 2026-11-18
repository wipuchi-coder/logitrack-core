from decimal import Decimal

from app.analytics import DemandRow, calculate_metrics


def row(product_id, value, demand=1, stock=10):
    return DemandRow(product_id, f"SKU-{product_id}", stock, demand, Decimal(value))


def test_abc_crossing_threshold_and_deterministic_ties():
    metrics = calculate_metrics([row(3, 5), row(1, 82), row(4, 5), row(2, 8)], 30, 7, 3)
    assert [(item.product_id, item.abc_class) for item in metrics] == [
        (1, "A"), (2, "B"), (3, "B"), (4, "C")
    ]


def test_reorder_uses_exact_demand_and_rounds_up():
    [item] = calculate_metrics([row(1, 20, demand=13, stock=2)], 90, 7, 3)
    assert item.average_daily_units == Decimal("0.1444")
    assert item.coverage_days == Decimal("13.85")
    assert item.reorder_point == 2
    assert item.reorder_needed


def test_zero_demand_has_no_coverage_or_reorder():
    [item] = calculate_metrics([row(1, 0, demand=0, stock=0)], 90, 7, 3)
    assert item.abc_class == "C"
    assert item.coverage_days is None
    assert item.reorder_point == 0
    assert not item.reorder_needed


def test_free_product_can_have_demand():
    [item] = calculate_metrics([row(1, 0, demand=30, stock=1)], 30, 1, 0)
    assert item.abc_class == "C"
    assert item.reorder_needed


def test_empty_warehouse():
    assert calculate_metrics([], 90, 7, 3) == []
