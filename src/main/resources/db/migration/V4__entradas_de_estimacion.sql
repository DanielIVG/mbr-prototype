-- V4: la estimacion guarda su modo y TODAS las cantidades de entrada con procedencia.
-- (V2 solo tenia p1_id y p2_id; en el modo "tasa x fraccion x barreras" P1 es un resultado.)
ALTER TABLE risk_estimation ADD COLUMN estimation_mode TEXT
    CHECK (estimation_mode IN ('P1_P2','FAILURE_MODE','TOTAL_ONLY'));
ALTER TABLE risk_estimation ADD COLUMN p1_value REAL;
ALTER TABLE risk_estimation ADD COLUMN p2_value REAL;

CREATE TABLE estimation_input (
    id                  INTEGER PRIMARY KEY,
    risk_estimation_id  INTEGER NOT NULL REFERENCES risk_estimation(id),
    role                TEXT NOT NULL CHECK (role IN ('P1','P2','RATE','FRACTION','BARRIER','TOTAL')),
    ordinal             INTEGER NOT NULL DEFAULT 1,
    quantity_id         INTEGER NOT NULL REFERENCES quantity(id),
    UNIQUE (risk_estimation_id, role, ordinal)
);
CREATE INDEX ix_estimation_input_quantity ON estimation_input(quantity_id);
