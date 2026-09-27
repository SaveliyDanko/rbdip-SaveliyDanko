-- Expand the schema first. Old application instances still read and write the
-- copied columns while new instances can use customer_id and product_id.

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
    ADD CONSTRAINT orders_customer_fk FOREIGN KEY (customer_id) REFERENCES customers(id);

CREATE INDEX orders_customer_id_idx ON orders (customer_id);

CREATE FUNCTION sync_order_customer() RETURNS trigger AS $$
BEGIN
    IF NEW.customer_id IS NULL THEN
        INSERT INTO customers (customer_full_name, customer_address, customer_phone)
        VALUES (NEW.customer_full_name, NEW.customer_address, NEW.customer_phone)
        ON CONFLICT (customer_full_name, customer_address, customer_phone)
        DO UPDATE SET customer_full_name = EXCLUDED.customer_full_name
        RETURNING id INTO NEW.customer_id;
    ELSE
        SELECT customer_full_name, customer_address, customer_phone
        INTO NEW.customer_full_name, NEW.customer_address, NEW.customer_phone
        FROM customers WHERE id = NEW.customer_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER orders_customer_compat
    BEFORE INSERT ON orders
    FOR EACH ROW EXECUTE FUNCTION sync_order_customer();

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
    ADD CONSTRAINT order_items_product_fk FOREIGN KEY (product_id) REFERENCES products(id);

CREATE INDEX order_items_product_id_idx ON order_items (product_id);

CREATE FUNCTION sync_order_item_product() RETURNS trigger AS $$
BEGIN
    IF NEW.product_id IS NULL THEN
        SELECT MIN(id) INTO NEW.product_id FROM products
        WHERE name = NEW.product_name AND price = NEW.product_price;
        IF NEW.product_id IS NULL THEN
            INSERT INTO products (name, price)
            VALUES (NEW.product_name, NEW.product_price)
            RETURNING id INTO NEW.product_id;
        END IF;
    ELSE
        SELECT name, price INTO NEW.product_name, NEW.product_price
        FROM products WHERE id = NEW.product_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_items_product_compat
    BEFORE INSERT ON order_items
    FOR EACH ROW EXECUTE FUNCTION sync_order_item_product();
