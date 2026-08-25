-- Perfil presentable del usuario: datos que la UI pinta en el shell/navbar.
-- Nullable por diseño: si no se conocen, la API los devuelve null en lugar
-- de fingirlos con el username.
ALTER TABLE users ADD COLUMN display_name VARCHAR(100);
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
