-- Start the contract after old application instances have been replaced.
-- Each contract migration locks one application table, avoiding a lock-order
-- cycle with concurrent order creation (customers -> orders -> order_items).
DROP TRIGGER IF EXISTS order_items_product_compat ON order_items;
DROP FUNCTION IF EXISTS sync_order_item_product();

ALTER TABLE order_items
    DROP COLUMN IF EXISTS product_name,
    DROP COLUMN IF EXISTS product_price;
