-- ============================================================
-- mvflix_storage: bibliotecas registradas en runtime por usuarios.
-- owner_username NULL -> biblioteca del operador (configurada por entorno);
-- no NULL           -> creada por un usuario final desde la UI.
-- ============================================================

ALTER TABLE media_libraries
    ADD COLUMN owner_username VARCHAR(64) NULL;