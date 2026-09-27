-- The final contract removes the legacy order columns after all old readers
-- have been replaced. It locks only orders, so writes cannot deadlock against
-- simultaneous locks on customers or order_items.
DROP TRIGGER IF EXISTS orders_customer_compat ON orders;
DROP FUNCTION IF EXISTS sync_order_customer();

ALTER TABLE orders
    DROP COLUMN IF EXISTS customer_full_name,
    DROP COLUMN IF EXISTS customer_address,
    DROP COLUMN IF EXISTS customer_phone;
