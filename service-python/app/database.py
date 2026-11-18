import os
from functools import lru_cache

from sqlalchemy import URL, create_engine


@lru_cache(maxsize=1)
def engine():
    url = URL.create(
        "postgresql+psycopg",
        username=os.getenv("POSTGRES_USER", "logitrack"),
        password=os.environ["POSTGRES_PASSWORD"],
        host=os.getenv("POSTGRES_HOST", "localhost"),
        port=int(os.getenv("POSTGRES_PORT", "5432")),
        database=os.getenv("POSTGRES_DB", "logitrack"),
    )
    return create_engine(
        url,
        pool_pre_ping=True,
        connect_args={"connect_timeout": 5},
        execution_options={"postgresql_readonly": True},
    )
