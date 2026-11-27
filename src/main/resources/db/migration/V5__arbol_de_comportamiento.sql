-- V5: arbol de comportamiento por componente (SI / NO).
-- Raiz = peligro; condicion = pregunta con ramas SI y NO; accion = paso con una sola salida;
-- resultado seguro = hoja sin riesgo; resultado peligroso = hoja que genera la cadena
-- (peligro -> pasos -> situacion peligrosa -> dano) con los datos de V1.
CREATE TABLE behavior_node (
    id                    INTEGER PRIMARY KEY,
    model_element_id      INTEGER NOT NULL REFERENCES model_element(id),
    parent_id             INTEGER REFERENCES behavior_node(id),
    root_id               INTEGER REFERENCES behavior_node(id),   -- NULL en una raiz
    kind                  TEXT NOT NULL CHECK (kind IN ('ACTION','CONDITION','OUTCOME_SAFE','OUTCOME_HAZARD')),
    description           TEXT NOT NULL CHECK (length(trim(description)) > 0),
    branch                TEXT CHECK (branch IN ('YES','NO')),
    situation_description TEXT,
    harm_description      TEXT,
    harm_type             TEXT,
    harm_id               INTEGER REFERENCES harm(id),            -- cadena generada por la hoja peligrosa
    active                INTEGER NOT NULL DEFAULT 1,
    created_date          TEXT NOT NULL,
    CHECK (parent_id IS NOT NULL OR (kind IN ('ACTION','CONDITION') AND branch IS NULL)),
    CHECK (kind <> 'OUTCOME_HAZARD' OR (harm_id IS NOT NULL
           AND length(trim(coalesce(situation_description,''))) > 0
           AND length(trim(coalesce(harm_description,''))) > 0)),
    CHECK (kind = 'OUTCOME_HAZARD' OR harm_id IS NULL)
);
CREATE INDEX ix_behavior_element ON behavior_node(model_element_id);
CREATE INDEX ix_behavior_parent  ON behavior_node(parent_id);
CREATE INDEX ix_behavior_root    ON behavior_node(root_id);
