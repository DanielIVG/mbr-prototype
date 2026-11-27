-- V7: cada componente nuevo de un dispositivo referencia un termino de la biblioteca (V6).
-- NULL solo en componentes creados antes de V7 (nombre libre); el servicio exige el codigo en los nuevos.
ALTER TABLE component ADD COLUMN library_code TEXT REFERENCES component_library(code);
CREATE INDEX ix_component_library_code ON component(library_code);
