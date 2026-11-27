-- =====================================================================
-- V2__iso14971_estimacion_y_fta.sql  --  Prototipo MBR (SQLite)
-- Capa ISO 14971: P1/P2, procedencia obligatoria, FTA con probabilidades
-- y trazabilidad por nodos (nivel 1).
-- Solo agrega: no modifica V1 (Flyway valida el checksum de V1).
-- GMDN fuera del alcance: gmdn_code y device.gmdn_code_id (V1) quedan
-- sin uso y sin sembrar. Se reemplazan por device_type (catalogo propio)
-- y device.external_nomenclature_ref (texto libre que escribe el usuario).
-- Reglas "de aplicacion" (validar unidades, independencia en compuertas Y,
-- P1/P2 o solo P total con constancia) se implementan en el servicio.
-- =====================================================================

-- ---------- Niveles de evidencia (PROPUESTA de escala propia, editable) ----------
CREATE TABLE evidence_level (
    id          INTEGER PRIMARY KEY,
    code        TEXT NOT NULL UNIQUE,
    descriptor  TEXT NOT NULL,
    rank        INTEGER NOT NULL UNIQUE          -- mayor = evidencia mas fuerte
);

INSERT INTO evidence_level (code, descriptor, rank) VALUES
    ('SUPUESTO',         'Supuesto sin respaldo documentado',                      1),
    ('JUICIO_EXPERTO',   'Juicio de experto documentado',                          2),
    ('FUENTE_PUBLICADA', 'Base de datos o literatura publicada',                   3),
    ('FABRICANTE',       'Dato del fabricante del componente (hoja de datos)',     4),
    ('MEDIDO',           'Medido en ensayo o verificacion del propio dispositivo', 5);

-- ---------- Cantidad con procedencia obligatoria ----------
-- Todo numero que alimenta una estimacion (tasa, fraccion, cobertura, P1, P2)
-- es una fila aqui: valor + unidad + fuente + nivel de evidencia + autor.
-- Sustituir = crear otra fila con supersedes_id; la anterior queda como historial.
CREATE TABLE quantity (
    id                 INTEGER PRIMARY KEY,
    value              REAL NOT NULL,
    unit               TEXT NOT NULL,                 -- unidad de exposicion o 'adimensional'
    range_min          REAL,                          -- rango para analisis de sensibilidad
    range_max          REAL,
    source             TEXT NOT NULL CHECK (length(trim(source)) > 0),
    evidence_level_id  INTEGER NOT NULL REFERENCES evidence_level(id),
    author             TEXT NOT NULL,
    created_date       TEXT NOT NULL,
    is_current         INTEGER NOT NULL DEFAULT 1 CHECK (is_current IN (0,1)),
    supersedes_id      INTEGER REFERENCES quantity(id),
    CHECK (range_min IS NULL OR range_max IS NULL OR range_min <= range_max)
);
CREATE UNIQUE INDEX ux_quantity_supersedes ON quantity(supersedes_id) WHERE supersedes_id IS NOT NULL;
CREATE INDEX ix_quantity_evidence ON quantity(evidence_level_id);

-- ---------- Tipo de dispositivo (catalogo propio, reemplaza a GMDN) ----------
CREATE TABLE device_type (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    source      TEXT NOT NULL,                        -- origen del catalogo (p. ej. IMDRF, EMDN, propio)
    source_ref  TEXT
);
ALTER TABLE device ADD COLUMN device_type_id INTEGER REFERENCES device_type(id);
ALTER TABLE device ADD COLUMN external_nomenclature_ref TEXT;
CREATE INDEX ix_device_type ON device(device_type_id);

-- ---------- Modos de falla con tasa y fraccion (con procedencia) ----------
ALTER TABLE failure_mode ADD COLUMN failure_rate_id  INTEGER REFERENCES quantity(id);
ALTER TABLE failure_mode ADD COLUMN mode_fraction_id INTEGER REFERENCES quantity(id);
CREATE INDEX ix_failure_mode_rate     ON failure_mode(failure_rate_id);
CREATE INDEX ix_failure_mode_fraction ON failure_mode(mode_fraction_id);

-- ---------- FTA: tipo de nodo, probabilidad, enlaces y trazabilidad ----------
ALTER TABLE fta_event ADD COLUMN event_type TEXT NOT NULL DEFAULT 'INTERMEDIATE'
    CHECK (event_type IN ('INTERMEDIATE','BASIC','UNDEVELOPED'));
ALTER TABLE fta_event ADD COLUMN probability_id  INTEGER REFERENCES quantity(id);          -- dato directo del evento basico
ALTER TABLE fta_event ADD COLUMN failure_mode_id INTEGER REFERENCES failure_mode(id);       -- hoja heredada de FMEA
ALTER TABLE fta_event ADD COLUMN control_measure_id INTEGER REFERENCES risk_control_measure(id); -- barrera (1 - cobertura)
ALTER TABLE fta_event ADD COLUMN model_element_id INTEGER REFERENCES model_element(id);      -- nivel 1: nodo <-> componente
ALTER TABLE fta_event ADD COLUMN independence_justification TEXT;                            -- exigida en compuertas Y (servicio)
CREATE INDEX ix_fta_event_probability ON fta_event(probability_id);
CREATE INDEX ix_fta_event_failure_mode ON fta_event(failure_mode_id);
CREATE INDEX ix_fta_event_control     ON fta_event(control_measure_id);
CREATE INDEX ix_fta_event_element     ON fta_event(model_element_id);

-- ---------- Estimacion ISO 14971: P1 x P2 ----------
-- probability_of_harm (V1) sigue siendo el NIVEL de la escala, derivado de p_total.
ALTER TABLE risk_estimation ADD COLUMN p1_id INTEGER REFERENCES quantity(id);
ALTER TABLE risk_estimation ADD COLUMN p2_id INTEGER REFERENCES quantity(id);
ALTER TABLE risk_estimation ADD COLUMN p_total REAL;
ALTER TABLE risk_estimation ADD COLUMN exposure_unit TEXT;
ALTER TABLE risk_estimation ADD COLUMN total_only_justification TEXT;   -- si no hay P1/P2 (RF-15)
ALTER TABLE risk_estimation ADD COLUMN fta_analysis_id INTEGER REFERENCES fta_analysis(id);
CREATE INDEX ix_estimation_p1  ON risk_estimation(p1_id);
CREATE INDEX ix_estimation_p2  ON risk_estimation(p2_id);
CREATE INDEX ix_estimation_fta ON risk_estimation(fta_analysis_id);

-- ---------- Escala de probabilidad: limites numericos por unidad ----------
ALTER TABLE probability_scale_level ADD COLUMN min_probability REAL;
ALTER TABLE probability_scale_level ADD COLUMN max_probability REAL;
ALTER TABLE probability_scale_level ADD COLUMN exposure_unit TEXT;
