-- 1. Enforce non-null version on inventory and add optimistic locking to orders
UPDATE inventory SET version = 0 WHERE version IS NULL;
ALTER TABLE inventory ALTER COLUMN version SET NOT NULL;
ALTER TABLE inventory ALTER COLUMN version SET DEFAULT 0;

ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 2. Database-level invariants against negative stock and zero/negative quantities
ALTER TABLE inventory ADD CONSTRAINT chk_inventory_stock_nonneg CHECK (stock >= 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_qty_pos CHECK (quantity > 0);

-- 3. Standardize product timestamps to TIMESTAMP WITH TIME ZONE (matching orders)
ALTER TABLE product
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING updated_at AT TIME ZONE 'UTC';