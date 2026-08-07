-- ---------- 1. Create roles ----------
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'db_migrator') THEN
CREATE ROLE db_migrator LOGIN PASSWORD '12345678';
END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'db_rw') THEN
CREATE ROLE db_rw LOGIN PASSWORD '12345678';
END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'db_ro') THEN
CREATE ROLE db_ro LOGIN PASSWORD '12345678';
END IF;
END
$$;