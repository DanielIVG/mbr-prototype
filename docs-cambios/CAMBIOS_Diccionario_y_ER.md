# Cambios al Diccionario de Datos / Paquete de Diseño (reassessment <-> rpn_factor)

## Tabla `rpn_factor` (secc. 2.4 Configuración FMEA / RPN)

| Columna | Tipo | Nulo | Clave | Referencia | Nota / restricción |
| --- | --- | --- | --- | --- | --- |
| id | INTEGER | No | PK | — | — |
| fmea_analysis_id | INTEGER | No | FK | fmea_analysis.id | Relación 1:N (una fila por evaluación/reevaluación, RF-29). Sin UNIQUE. |
| severity | INTEGER | No | — | — | — |
| occurrence | INTEGER | No | — | — | — |
| detection | INTEGER | No | — | — | — |
| rpn | INTEGER | No | — | — | CHECK (rpn = severity × occurrence × detection) (RF-24). |
| severity_level_id | INTEGER | No | FK | fmea_scale_level.id | factor_type=SEVERIDAD (regla de aplicación). |
| occurrence_level_id | INTEGER | No | FK | fmea_scale_level.id | factor_type=OCURRENCIA. |
| detection_level_id | INTEGER | No | FK | fmea_scale_level.id | factor_type=DETECCION. |
| priority_category_id | INTEGER | Sí | FK | rpn_priority_category.id | (sin cambio) |
| is_current | INTEGER (0/1) | No | — | — | DEFAULT 1. Índice único parcial `ux_rpn_factor_current ON rpn_factor(fmea_analysis_id) WHERE is_current = 1`: un solo registro vigente por análisis; los demás son historial. |
| reassessment_id | INTEGER | Sí | FK | reassessment.id | NUEVO. Reevaluación que originó este registro; NULL en la evaluación inicial. Índice `ix_rpn_factor_reassessment`. |

## Tabla `reassessment` (secc. 2.7 Historial)
ELIMINAR la fila: `rpn_factor_id | INTEGER | Sí | FK | rpn_factor.id | Solo si el riesgo tiene FMEA asociado (RF-29).`
(La relación ahora se obtiene por rpn_factor.reassessment_id.)

## Diagrama ER / clases
- fmea_analysis 1 ── 0..* rpn_factor (antes 1 ── 1).
- Quitar la asociación reassessment → rpn_factor.
- Agregar asociación rpn_factor 0..* ── 0..1 reassessment (FK en rpn_factor).
- Atributo nuevo en RPNFactor: isCurrent : boolean; reassessmentId : Long (opcional).

## Nota de implementación
Verificado en código: ninguna clase Java ni prueba usa `rpn_factor_id`. Hasta que RF-29 (Reassessment) se implemente, rpn_factor.reassessment_id queda en NULL; FmeaService.reevaluate hoy solo conmuta is_current.
