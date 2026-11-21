-- Preserve product names on historical order lines and store order timestamps as instants.
ALTER TABLE order_items ADD COLUMN product_name VARCHAR(255);

UPDATE order_items oi
SET product_name = p.name
FROM product p
WHERE oi.product_id = p.id;

ALTER TABLE order_items ALTER COLUMN product_name SET NOT NULL;

ALTER TABLE orders
    ALTER COLUMN order_date TYPE TIMESTAMP WITH TIME ZONE
    USING order_date AT TIME ZONE 'UTC';

ALTER TABLE orders
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE
    USING updated_at AT TIME ZONE 'UTC';
