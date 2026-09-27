-- New application instances use first_name/last_name. The old full name
-- remains readable and writable until the final contract migration.
ALTER TABLE customers
    ADD COLUMN first_name VARCHAR(255),
    ADD COLUMN last_name VARCHAR(255);

UPDATE customers
SET first_name = split_part(customer_full_name, ' ', 1),
    last_name = CASE
        WHEN position(' ' IN customer_full_name) = 0 THEN NULL
        ELSE substring(customer_full_name FROM position(' ' IN customer_full_name) + 1)
    END;

CREATE FUNCTION sync_customer_name() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.first_name IS DISTINCT FROM OLD.first_name
                OR NEW.last_name IS DISTINCT FROM OLD.last_name THEN
            NEW.customer_full_name := NEW.first_name
                || CASE WHEN NEW.last_name IS NULL THEN '' ELSE ' ' || NEW.last_name END;
        ELSIF NEW.customer_full_name IS DISTINCT FROM OLD.customer_full_name THEN
            NEW.first_name := split_part(NEW.customer_full_name, ' ', 1);
            NEW.last_name := CASE
                WHEN position(' ' IN NEW.customer_full_name) = 0 THEN NULL
                ELSE substring(NEW.customer_full_name FROM position(' ' IN NEW.customer_full_name) + 1)
            END;
        END IF;
    ELSIF NEW.customer_full_name IS NULL THEN
        NEW.customer_full_name := NEW.first_name
            || CASE WHEN NEW.last_name IS NULL THEN '' ELSE ' ' || NEW.last_name END;
    ELSE
        NEW.first_name := split_part(NEW.customer_full_name, ' ', 1);
        NEW.last_name := CASE
            WHEN position(' ' IN NEW.customer_full_name) = 0 THEN NULL
            ELSE substring(NEW.customer_full_name FROM position(' ' IN NEW.customer_full_name) + 1)
        END;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER customers_name_compat
    BEFORE INSERT OR UPDATE OF customer_full_name, first_name, last_name ON customers
    FOR EACH ROW EXECUTE FUNCTION sync_customer_name();

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
