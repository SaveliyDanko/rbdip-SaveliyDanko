-- Keep existing orders and items while replacing copied contact/product data
-- with foreign keys. Flyway runs this migration in one PostgreSQL transaction.

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    customer_full_name VARCHAR(255) NOT NULL,
    customer_address VARCHAR(500),
    customer_phone VARCHAR(50)
);

INSERT INTO customers (customer_full_name, customer_address, customer_phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone
FROM orders;

CREATE UNIQUE INDEX customers_contact_unique
    ON customers (customer_full_name, customer_address, customer_phone) NULLS NOT DISTINCT;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;

UPDATE orders AS o
SET customer_id = c.id
FROM customers AS c
WHERE o.customer_full_name = c.customer_full_name
  AND o.customer_address IS NOT DISTINCT FROM c.customer_address
  AND o.customer_phone IS NOT DISTINCT FROM c.customer_phone;

ALTER TABLE orders
    ALTER COLUMN customer_id SET NOT NULL,
    ADD CONSTRAINT orders_customer_fk FOREIGN KEY (customer_id) REFERENCES customers(id),
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

CREATE INDEX orders_customer_id_idx ON orders (customer_id);

ALTER TABLE order_items ADD COLUMN product_id BIGINT;

-- Reuse a matching product when possible; create one for historical items
-- whose product no longer exists or whose price has since changed.
INSERT INTO products (name, price)
SELECT DISTINCT oi.product_name, oi.product_price
FROM order_items AS oi
WHERE NOT EXISTS (
    SELECT 1 FROM products AS p
    WHERE p.name = oi.product_name AND p.price = oi.product_price
);

UPDATE order_items AS oi
SET product_id = (
    SELECT MIN(p.id)
    FROM products AS p
    WHERE p.name = oi.product_name AND p.price = oi.product_price
);

ALTER TABLE order_items
    ALTER COLUMN product_id SET NOT NULL,
    ADD CONSTRAINT order_items_product_fk FOREIGN KEY (product_id) REFERENCES products(id),
    DROP COLUMN product_name,
    DROP COLUMN product_price;

CREATE INDEX order_items_product_id_idx ON order_items (product_id);
