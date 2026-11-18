import logging
from typing import Annotated

from fastapi import FastAPI, HTTPException, Path, Query
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from .analytics import calculate_metrics, load_demand
from .database import engine
from .schemas import WarehouseAnalytics

app = FastAPI(title="LogiTrack Core | Analítica de inventario", version="1.0.0")
logger = logging.getLogger(__name__)


@app.get("/health", tags=["Operación"])
def health() -> dict[str, str]:
    try:
        with engine().connect() as connection:
            connection.execute(text("SELECT id FROM warehouses LIMIT 1"))
    except SQLAlchemyError as error:
        logger.warning("PostgreSQL no disponible para analítica (%s)", type(error).__name__)
        raise HTTPException(status_code=503, detail="Base de datos no disponible") from error
    return {"status": "up"}


@app.get("/api/analytics/warehouses/{warehouse_id}", tags=["Analítica"])
def warehouse_analytics(
    warehouse_id: Annotated[int, Path(gt=0, le=2**63 - 1)],
    window_days: Annotated[int, Query(ge=1, le=365)] = 90,
    lead_time_days: Annotated[int, Query(ge=0, le=365)] = 7,
    safety_days: Annotated[int, Query(ge=0, le=365)] = 3,
) -> WarehouseAnalytics:
    try:
        with engine().connect() as connection:
            exists = connection.execute(
                text("SELECT 1 FROM warehouses WHERE id = :id"), {"id": warehouse_id}
            ).scalar_one_or_none()
            if exists is None:
                raise HTTPException(status_code=404, detail="Almacén inexistente")
            rows = load_demand(connection, warehouse_id, window_days)
    except SQLAlchemyError as error:
        logger.warning("Consulta de demanda fallida (%s)", type(error).__name__)
        raise HTTPException(status_code=503, detail="Analítica temporalmente no disponible") from error

    return WarehouseAnalytics(
        warehouse_id=warehouse_id,
        window_days=window_days,
        lead_time_days=lead_time_days,
        safety_days=safety_days,
        items=calculate_metrics(rows, window_days, lead_time_days, safety_days),
    )
