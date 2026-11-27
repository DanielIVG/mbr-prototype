-- V10: vincular modo de falla a nodo del árbol de comportamiento.
-- Permite que el FMEA sea fuente de eventos en la secuencia de riesgo.
-- La ocurrencia del modo de falla ayuda a estimar P1 junto con el FTA.

ALTER TABLE failure_mode ADD COLUMN behavior_node_id INTEGER REFERENCES behavior_node(id);
CREATE INDEX ix_failure_mode_behavior_node ON failure_mode(behavior_node_id);
