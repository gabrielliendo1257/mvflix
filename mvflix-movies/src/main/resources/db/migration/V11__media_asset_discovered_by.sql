-- Origen del descubrimiento: quién pidió el scan que trajo el archivo.
-- Nullable por diseño: los assets anteriores al sello quedan "huérfanos" y
-- solo un admin puede gestionarlos/listarlos.
ALTER TABLE media_assets ADD COLUMN discovered_by VARCHAR(50);
