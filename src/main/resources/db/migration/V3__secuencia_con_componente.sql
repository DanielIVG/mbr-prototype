-- V3: cada paso de la secuencia de eventos puede indicar el componente donde ocurre.
-- Permite dibujar el "camino de la falla" (origen -> pasos -> situacion -> dano).
ALTER TABLE sequence_of_events ADD COLUMN model_element_id INTEGER REFERENCES model_element(id);
CREATE INDEX ix_sequence_element ON sequence_of_events(model_element_id);
