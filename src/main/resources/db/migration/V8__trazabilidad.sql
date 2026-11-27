-- V8: trazabilidad requisito -> medida de control -> verificacion (prueba) -> dano.
-- Una medida de control de riesgo se implementa como requisito (ISO 14971, 7.1) y se prueba con una
-- verificacion; por eso el requisito se enlaza a la medida de control y no directamente al dano.
CREATE TABLE requirement (
    id            INTEGER PRIMARY KEY,
    device_id     INTEGER NOT NULL REFERENCES device(id),
    code          TEXT NOT NULL,
    description   TEXT NOT NULL,
    type          TEXT NOT NULL CHECK (type IN ('FUNCIONAL','SEGURIDAD','USABILIDAD','DESEMPENO','REGULATORIO')),
    source        TEXT,
    active        INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
    created_date  TEXT NOT NULL
);
-- El codigo es unico entre los requisitos vigentes de un dispositivo.
CREATE UNIQUE INDEX ux_requirement_code ON requirement(device_id, code) WHERE active = 1;
CREATE INDEX ix_requirement_device ON requirement(device_id);

CREATE TABLE requirement_control (
    requirement_id      INTEGER NOT NULL REFERENCES requirement(id),
    control_measure_id  INTEGER NOT NULL REFERENCES risk_control_measure(id),
    PRIMARY KEY (requirement_id, control_measure_id)
);
CREATE INDEX ix_reqctl_control ON requirement_control(control_measure_id);
