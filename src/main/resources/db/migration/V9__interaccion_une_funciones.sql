-- V9: las interacciones unen funciones (no componentes directamente).
-- Los componentes que intervienen se derivan de las funciones involucradas.
-- Se mantiene interaction_component para compatibilidad con datos existentes.

CREATE TABLE interaction_function (
    interaction_id  INTEGER NOT NULL REFERENCES interaction(id),
    function_id     INTEGER NOT NULL REFERENCES function(id),
    PRIMARY KEY (interaction_id, function_id)
);
CREATE INDEX ix_interaction_function_func ON interaction_function(function_id);

-- Migrar datos existentes: para cada interacción con componentes,
-- buscar funciones que usen al menos uno de esos componentes.
INSERT INTO interaction_function (interaction_id, function_id)
SELECT DISTINCT ic.interaction_id, fc.function_id
FROM interaction_component ic
JOIN function_component fc ON fc.component_id = ic.component_id
JOIN function f ON f.id = fc.function_id
JOIN interaction i ON i.id = ic.interaction_id
WHERE f.device_id = i.device_id;
