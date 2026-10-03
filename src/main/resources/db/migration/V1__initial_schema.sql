-- =====================================================================
-- V1__initial_schema.sql  --  Prototipo MBR (SQLite)
-- Fuente: ER_fisico_MBR.drawio + Diccionario_de_Datos_MBR (30 tablas)
-- Herencia JOINED: model_element (device, function, component,
-- interaction) y analysis (fha_analysis, fta_analysis, fmea_analysis).
-- Booleanos = INTEGER 0/1. Fechas = TEXT ISO-8601.
-- Cambio respecto al ER original: rpn_factor es 1:N con fmea_analysis
-- (historial RF-29) y agrega is_current ([JC-5], [JC-6]).
-- Reglas "de aplicacion" del diccionario NO se implementan aqui.
-- Requiere foreign_keys=ON en cada conexion (no se puede fijar aqui).
-- =====================================================================

-- ---------- 2.1 Modelo del sistema ----------
CREATE TABLE gmdn_code (
    id          INTEGER PRIMARY KEY,
    code        TEXT NOT NULL UNIQUE,
    term_name   TEXT NOT NULL,
    definition  TEXT
);

CREATE TABLE model_element (
    id      INTEGER PRIMARY KEY,
    active  INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1))
);

CREATE TABLE device (
    id                              INTEGER PRIMARY KEY REFERENCES model_element(id),
    gmdn_code_id                    INTEGER REFERENCES gmdn_code(id),
    name                            TEXT NOT NULL,
    intended_use                    TEXT NOT NULL,
    reasonably_foreseeable_misuse   TEXT
);

CREATE TABLE function (
    id           INTEGER PRIMARY KEY REFERENCES model_element(id),
    device_id    INTEGER NOT NULL REFERENCES device(id),
    name         TEXT NOT NULL,
    description  TEXT
);

CREATE TABLE component (
    id                       INTEGER PRIMARY KEY REFERENCES model_element(id),
    device_id                INTEGER NOT NULL REFERENCES device(id),
    parent_component_id      INTEGER REFERENCES component(id),
    name                     TEXT NOT NULL,
    description              TEXT,
    essential                INTEGER NOT NULL DEFAULT 0 CHECK (essential IN (0,1)),
    essential_justification  TEXT
);

CREATE TABLE interaction (
    id                       INTEGER PRIMARY KEY REFERENCES model_element(id),
    device_id                INTEGER NOT NULL REFERENCES device(id),
    description              TEXT NOT NULL,
    relevance_justification  TEXT NOT NULL
);

CREATE TABLE function_component (
    function_id   INTEGER NOT NULL REFERENCES function(id),
    component_id  INTEGER NOT NULL REFERENCES component(id),
    PRIMARY KEY (function_id, component_id)
);

CREATE TABLE interaction_component (
    interaction_id  INTEGER NOT NULL REFERENCES interaction(id),
    component_id    INTEGER NOT NULL REFERENCES component(id),
    PRIMARY KEY (interaction_id, component_id)
);

-- ---------- 2.6 Configuracion ISO 14971 (antes de las tablas que la usan) ----------
CREATE TABLE severity_scale_level (
    id          INTEGER PRIMARY KEY,
    value       INTEGER NOT NULL,
    descriptor  TEXT NOT NULL
);

CREATE TABLE probability_scale_level (
    id          INTEGER PRIMARY KEY,
    value       INTEGER NOT NULL,
    descriptor  TEXT NOT NULL
);

CREATE TABLE risk_matrix_cell (
    id                    INTEGER PRIMARY KEY,
    severity_level_id     INTEGER NOT NULL REFERENCES severity_scale_level(id),
    probability_level_id  INTEGER NOT NULL REFERENCES probability_scale_level(id),
    risk_level            TEXT NOT NULL,
    UNIQUE (severity_level_id, probability_level_id)
);

CREATE TABLE acceptability_criterion (
    id             INTEGER PRIMARY KEY,
    risk_level     TEXT NOT NULL,
    acceptable     INTEGER NOT NULL CHECK (acceptable IN (0,1)),
    justification  TEXT
);

-- ---------- 2.2 Cadena de riesgo ----------
CREATE TABLE hazard (
    id                INTEGER PRIMARY KEY,
    model_element_id  INTEGER REFERENCES model_element(id),
    description       TEXT NOT NULL,
    source            TEXT,
    review_status     TEXT
);

CREATE TABLE sequence_of_events (
    id           INTEGER PRIMARY KEY,
    hazard_id    INTEGER NOT NULL REFERENCES hazard(id),
    description  TEXT NOT NULL,
    step_order   INTEGER NOT NULL
);

CREATE TABLE hazardous_situation (
    id                     INTEGER PRIMARY KEY,
    sequence_of_events_id  INTEGER NOT NULL REFERENCES sequence_of_events(id),
    description            TEXT NOT NULL
);

CREATE TABLE harm (
    id                      INTEGER PRIMARY KEY,
    hazardous_situation_id  INTEGER NOT NULL REFERENCES hazardous_situation(id),
    description             TEXT NOT NULL,
    type                    TEXT
);

CREATE TABLE failure_mode (
    id                INTEGER PRIMARY KEY,
    model_element_id  INTEGER REFERENCES model_element(id),
    description       TEXT NOT NULL
);

-- ---------- 2.3 Analisis multitecnica ----------
CREATE TABLE analysis (
    id            INTEGER PRIMARY KEY,
    harm_id       INTEGER NOT NULL REFERENCES harm(id),
    analysis_type TEXT NOT NULL CHECK (analysis_type IN ('FHA','FTA','FMEA')),
    created_date  TEXT NOT NULL,
    source        TEXT,
    active        INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1))
);

CREATE TABLE fha_analysis (
    id                 INTEGER PRIMARY KEY REFERENCES analysis(id),
    function_id        INTEGER NOT NULL REFERENCES function(id),
    functional_hazard  TEXT NOT NULL,
    effects            TEXT NOT NULL
);

CREATE TABLE fta_analysis (
    id                     INTEGER PRIMARY KEY REFERENCES analysis(id),
    top_event_description  TEXT NOT NULL
);

CREATE TABLE fta_event (
    id                 INTEGER PRIMARY KEY,
    fta_analysis_id    INTEGER NOT NULL REFERENCES fta_analysis(id),
    parent_event_id    INTEGER REFERENCES fta_event(id),
    description        TEXT NOT NULL,
    logical_gate_type  TEXT CHECK (logical_gate_type IN ('AND','OR')),
    is_top_event       INTEGER NOT NULL DEFAULT 0 CHECK (is_top_event IN (0,1))
);

CREATE TABLE fmea_analysis (
    id                 INTEGER PRIMARY KEY REFERENCES analysis(id),
    failure_mode_id    INTEGER NOT NULL REFERENCES failure_mode(id),
    effect             TEXT NOT NULL,
    cause              TEXT,
    detection_method   TEXT,
    existing_controls  TEXT
);

-- ---------- 2.4 Configuracion FMEA / RPN ----------
CREATE TABLE fmea_scale_level (
    id           INTEGER PRIMARY KEY,
    factor_type  TEXT NOT NULL CHECK (factor_type IN ('SEVERIDAD','OCURRENCIA','DETECCION')),
    value        INTEGER NOT NULL,
    descriptor   TEXT NOT NULL,
    UNIQUE (factor_type, value)                                  -- [JC-2]
);

CREATE TABLE rpn_priority_category (
    id             INTEGER PRIMARY KEY,
    name           TEXT NOT NULL,
    min_rpn        INTEGER NOT NULL,
    max_rpn        INTEGER NOT NULL,
    justification  TEXT NOT NULL,
    CHECK (min_rpn <= max_rpn)                                   -- [JC-1]
);

CREATE TABLE rpn_factor (
    id                    INTEGER PRIMARY KEY,
    fmea_analysis_id      INTEGER NOT NULL REFERENCES fmea_analysis(id),  -- [JC-5] sin UNIQUE: una fila por reevaluacion (RF-29)
    severity              INTEGER NOT NULL,
    occurrence            INTEGER NOT NULL,
    detection             INTEGER NOT NULL,
    rpn                   INTEGER NOT NULL,
    severity_level_id     INTEGER NOT NULL REFERENCES fmea_scale_level(id),
    occurrence_level_id   INTEGER NOT NULL REFERENCES fmea_scale_level(id),
    detection_level_id    INTEGER NOT NULL REFERENCES fmea_scale_level(id),
    priority_category_id  INTEGER REFERENCES rpn_priority_category(id),
    is_current            INTEGER NOT NULL DEFAULT 1 CHECK (is_current IN (0,1)),  -- [JC-6]
    CHECK (rpn = severity * occurrence * detection)              -- [JC-3]
);

-- [JC-6] Un solo registro vigente por analisis FMEA; los demas son historial.
CREATE UNIQUE INDEX ux_rpn_factor_current ON rpn_factor(fmea_analysis_id) WHERE is_current = 1;

-- ---------- 2.5 Estimacion y control ----------
CREATE TABLE risk_estimation (
    id                    INTEGER PRIMARY KEY,
    harm_id               INTEGER NOT NULL REFERENCES harm(id),
    severity              INTEGER NOT NULL,
    probability_of_harm   INTEGER NOT NULL,
    date                  TEXT NOT NULL,
    severity_level_id     INTEGER NOT NULL REFERENCES severity_scale_level(id),
    probability_level_id  INTEGER NOT NULL REFERENCES probability_scale_level(id)
);

CREATE TABLE risk_evaluation (
    id                          INTEGER PRIMARY KEY,
    risk_estimation_id          INTEGER NOT NULL UNIQUE REFERENCES risk_estimation(id),
    matrix_cell_id              INTEGER NOT NULL REFERENCES risk_matrix_cell(id),
    acceptability_criterion_id  INTEGER NOT NULL REFERENCES acceptability_criterion(id),
    risk_level                  TEXT NOT NULL,
    acceptable                  INTEGER NOT NULL CHECK (acceptable IN (0,1)),
    date                        TEXT NOT NULL
);

CREATE TABLE risk_control_measure (
    id           INTEGER PRIMARY KEY,
    harm_id      INTEGER NOT NULL REFERENCES harm(id),
    description  TEXT NOT NULL,
    type         TEXT
);

CREATE TABLE verification (
    id                  INTEGER PRIMARY KEY,
    control_measure_id  INTEGER NOT NULL REFERENCES risk_control_measure(id),
    method              TEXT NOT NULL,
    implemented         INTEGER NOT NULL CHECK (implemented IN (0,1)),
    effective           INTEGER CHECK (effective IN (0,1)),
    date                TEXT NOT NULL,
    evidence            TEXT
);

-- ---------- 2.7 Historial ----------
CREATE TABLE reassessment (
    id                    INTEGER PRIMARY KEY,
    harm_id               INTEGER NOT NULL REFERENCES harm(id),
    date                  TEXT NOT NULL,
    trigger_reason        TEXT,
    estimation_before_id  INTEGER NOT NULL REFERENCES risk_estimation(id),
    estimation_after_id   INTEGER NOT NULL REFERENCES risk_estimation(id),
    evaluation_before_id  INTEGER NOT NULL REFERENCES risk_evaluation(id),
    evaluation_after_id   INTEGER NOT NULL REFERENCES risk_evaluation(id),
    rpn_factor_id         INTEGER REFERENCES rpn_factor(id)
);

-- ---------- Indices en FK (SQLite no los crea solo) [JC-4] ----------
CREATE INDEX ix_function_device            ON function(device_id);
CREATE INDEX ix_component_device           ON component(device_id);
CREATE INDEX ix_component_parent           ON component(parent_component_id);
CREATE INDEX ix_interaction_device         ON interaction(device_id);
CREATE INDEX ix_function_component_comp    ON function_component(component_id);
CREATE INDEX ix_interaction_component_comp ON interaction_component(component_id);
CREATE INDEX ix_hazard_element             ON hazard(model_element_id);
CREATE INDEX ix_sequence_hazard            ON sequence_of_events(hazard_id);
CREATE INDEX ix_situation_sequence         ON hazardous_situation(sequence_of_events_id);
CREATE INDEX ix_harm_situation             ON harm(hazardous_situation_id);
CREATE INDEX ix_failure_mode_element       ON failure_mode(model_element_id);
CREATE INDEX ix_analysis_harm              ON analysis(harm_id);
CREATE INDEX ix_fha_function               ON fha_analysis(function_id);
CREATE INDEX ix_fta_event_analysis         ON fta_event(fta_analysis_id);
CREATE INDEX ix_fta_event_parent           ON fta_event(parent_event_id);
CREATE INDEX ix_fmea_failure_mode          ON fmea_analysis(failure_mode_id);
CREATE INDEX ix_rpn_category               ON rpn_factor(priority_category_id);
CREATE INDEX ix_rpn_fmea                   ON rpn_factor(fmea_analysis_id);
CREATE INDEX ix_estimation_harm            ON risk_estimation(harm_id);
CREATE INDEX ix_control_harm               ON risk_control_measure(harm_id);
CREATE INDEX ix_verification_control       ON verification(control_measure_id);
CREATE INDEX ix_reassessment_harm          ON reassessment(harm_id);
