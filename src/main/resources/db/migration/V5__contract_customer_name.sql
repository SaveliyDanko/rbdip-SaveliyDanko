-- Replace the compatibility function before removing customers.customer_full_name.
-- orders.customer_full_name remains available to old readers until V6.
CREATE UNIQUE INDEX customers_name_unique
    ON customers (first_name, last_name, customer_address, customer_phone) NULLS NOT DISTINCT;

CREATE OR REPLACE FUNCTION sync_order_customer() RETURNS trigger AS $$
BEGIN
    IF NEW.customer_id IS NULL THEN
        INSERT INTO customers (first_name, last_name, customer_address, customer_phone)
        VALUES (
            split_part(NEW.customer_full_name, ' ', 1),
            CASE
                WHEN position(' ' IN NEW.customer_full_name) = 0 THEN NULL
                ELSE substring(NEW.customer_full_name FROM position(' ' IN NEW.customer_full_name) + 1)
            END,
            NEW.customer_address,
            NEW.customer_phone
        )
        ON CONFLICT (first_name, last_name, customer_address, customer_phone)
        DO UPDATE SET first_name = EXCLUDED.first_name
        RETURNING id INTO NEW.customer_id;
    ELSE
        SELECT first_name || CASE WHEN last_name IS NULL THEN '' ELSE ' ' || last_name END,
               customer_address, customer_phone
        INTO NEW.customer_full_name, NEW.customer_address, NEW.customer_phone
        FROM customers WHERE id = NEW.customer_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER customers_name_compat ON customers;
DROP FUNCTION sync_customer_name();
DROP INDEX customers_contact_unique;
ALTER TABLE customers DROP COLUMN customer_full_name;
