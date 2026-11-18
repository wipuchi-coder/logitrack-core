BEGIN;

CREATE TABLE warehouses (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(32) NOT NULL UNIQUE CHECK (btrim(code) <> ''),
    name varchar(120) NOT NULL CHECK (btrim(name) <> '')
);
CREATE TABLE products (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku varchar(48) NOT NULL UNIQUE CHECK (btrim(sku) <> ''),
    name varchar(120) NOT NULL CHECK (btrim(name) <> ''),
    unit_cost numeric(12,2) NOT NULL CHECK (unit_cost >= 0)
);
CREATE TABLE inventory (
    warehouse_id bigint REFERENCES warehouses,
    product_id bigint REFERENCES products,
    quantity bigint NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (warehouse_id, product_id)
);
CREATE TABLE movements (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id bigint NOT NULL REFERENCES warehouses,
    product_id bigint NOT NULL REFERENCES products,
    kind varchar(3) NOT NULL CHECK (kind IN ('IN', 'OUT')),
    quantity integer NOT NULL CHECK (quantity > 0),
    unit_cost numeric(12,2) NOT NULL CHECK (unit_cost >= 0),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    event_id uuid UNIQUE,
    epc varchar(64),
    CONSTRAINT rfid_metadata CHECK (
        (event_id IS NULL AND epc IS NULL) OR
        (event_id IS NOT NULL AND epc IS NOT NULL AND btrim(epc) <> '')
    )
);
CREATE INDEX movements_demand_idx ON movements (occurred_at, warehouse_id, product_id)
    WHERE kind = 'OUT';
CREATE INDEX movements_history_idx ON movements (warehouse_id, product_id, occurred_at DESC, id DESC);
CREATE INDEX inventory_product_idx ON inventory (product_id);

CREATE FUNCTION apply_movement() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.kind = 'IN' THEN
        INSERT INTO inventory (warehouse_id, product_id, quantity)
        VALUES (NEW.warehouse_id, NEW.product_id, NEW.quantity)
        ON CONFLICT (warehouse_id, product_id)
        DO UPDATE SET quantity = inventory.quantity + EXCLUDED.quantity;
    ELSE
        UPDATE inventory SET quantity = quantity - NEW.quantity
        WHERE warehouse_id = NEW.warehouse_id AND product_id = NEW.product_id
          AND quantity >= NEW.quantity;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Saldo insuficiente' USING ERRCODE = 'LT001';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER movements_apply AFTER INSERT ON movements
    FOR EACH ROW EXECUTE FUNCTION apply_movement();

CREATE FUNCTION reject_movement_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'El historial de movimientos es inmutable';
END;
$$;
CREATE TRIGGER movements_immutable BEFORE UPDATE OR DELETE OR TRUNCATE ON movements
    FOR EACH STATEMENT EXECUTE FUNCTION reject_movement_change();
COMMIT;
