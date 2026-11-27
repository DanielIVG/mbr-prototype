# Informe de Revision Normativa: Mejoras Visuales de la Interfaz

**Producto:** MBR - Prototipo de gestion de riesgos basado en modelos para dispositivos medicos
**Version:** 1.0
**Fecha:** 2026-11-27
**Responsable:** Revisor Normativo
**Documentos revisados:**
- Especificacion: `docs/specs/mejoras-visuales.md`
- Diseno tecnico: `docs/design/mejoras-visuales.md`
- CSS actual: `src/main/resources/css/mbr.css`
**Marco normativo de referencia:** ISO 14971:2019

---

## 1. Resumen Ejecutivo

**Veredicto: APROBADO**

Los cambios propuestos en la especificacion y diseno tecnico de mejoras visuales son **puramente de presentacion** (CSS y estructura de componentes UI) y **no afectan**:

1. La logica de evaluacion de riesgos (`RiskAssessor`, `RiskMatrix`, `RiskEstimator`)
2. El modelo de datos subyacente (cadenas de riesgo, estimaciones, controles)
3. La trazabilidad peligro - situacion peligrosa - dano - riesgo - control - verificacion
4. Los criterios de aceptabilidad definidos en la matriz de riesgo

Los cambios visuales **mejoran** la usabilidad del sistema para la gestion de riesgos al:
- Incrementar el contraste y legibilidad de indicadores de estado (tags de riesgo)
- Estandarizar el tamano minimo de fuente (12px) para cumplir con accesibilidad basica
- Mejorar la diferenciacion visual entre estados aceptable/no aceptable

---

## 2. Hallazgos

### 2.1 Hallazgos CRITICOS

**Ninguno identificado.**

Los cambios propuestos no introducen riesgos en:
- La integridad de los datos de gestion de riesgos
- La logica de calculo de probabilidad o evaluacion contra la matriz
- La trazabilidad de requisitos a medidas de control

### 2.2 Hallazgos OBSERVACION

#### OBS-01: Ajuste de tamano de fuente en indicadores de estado

**Descripcion:** El diseno propone cambiar el tamano de fuente de `.tag`, `.tag-ok`, `.tag-bad`, `.tag-warn` de 11px a 12px (REQ-VIS-020, Tarea T-2).

**Codigo actual (mbr.css, lineas 155-158, 177-180, 237-244):**
```css
.tag { ... -fx-font-size: 11px; }
.tag-bad { ... -fx-font-size: 11px; }
.tag-ok { ... -fx-font-size: 11px; }
.tag-warn { ... -fx-font-size: 11px; }
```

**Impacto normativo:** Ninguno. El cambio mejora la legibilidad de los indicadores de riesgo sin alterar su semantica.

**Clausula relacionada:** No aplica directamente a ISO 14971. Este es un tema de usabilidad/IEC 62366 (usabilidad de dispositivos medicos), aunque el sistema MBR no es en si un dispositivo medico sino una herramienta de soporte.

**Estado:** Aceptable, sin accion requerida.

---

#### OBS-02: Colores de indicadores de severidad

**Descripcion:** Los colores semanticos utilizados para indicar estados de riesgo estan bien definidos y son distinguibles:

| Clase | Color fondo | Color texto | Uso |
|-------|-------------|-------------|-----|
| `.tag-ok` | #DDF0E4 (verde claro) | #0E6B3A (verde oscuro) | Riesgo aceptable |
| `.tag-bad` | #F8DCDC (rojo claro) | #7A1F1F (rojo oscuro) | Riesgo no aceptable |
| `.tag-warn` | #FBEFD3 (amarillo claro) | #7A5300 (marron) | Advertencia/sin medida |
| `.tag` | #DCEBED (teal claro) | #0E434B (teal oscuro) | Neutral/informativo |
| `.verdict-ok` | N/A | #0E6B3A (verde oscuro) | Veredicto aceptable |
| `.verdict-bad` | N/A | #9B2C2C (rojo oscuro) | Veredicto no aceptable |

**Verificacion de uso en codigo:**
- `AssessmentView.java:173`: Usa `"tag"` para riesgo aceptable y `"tag-bad"` para no aceptable
- `AssessmentView.java:262`: Usa `"verdict-ok"` y `"verdict-bad"` para el veredicto final
- `TraceabilityView.java:168`: Usa `"tag-warn"` para requisitos sin medida de control y `"tag-ok"` para los que tienen

**Impacto normativo:** Positivo. La diferenciacion clara entre estados aceptable/no aceptable apoya el proceso de evaluacion de riesgos segun ISO 14971:2019 clausula 7 (Evaluacion del riesgo).

**Estado:** Aceptable.

---

#### OBS-03: El diseno no modifica la logica de evaluacion

**Descripcion:** Se verifico que los archivos de logica de riesgos no estan incluidos en el alcance de cambios:

- `src/main/java/org/example/iso/RiskAssessor.java` - Sin cambios
- `src/main/java/org/example/iso/RiskMatrix.java` - Sin cambios
- `src/main/java/org/example/iso/RiskEstimator.java` - Sin cambios
- `src/main/java/org/example/iso/Sensitivity.java` - Sin cambios

**Clausula relacionada:** ISO 14971:2019 clausulas 4.5 (Archivo de gestion de riesgos), 7 (Evaluacion del riesgo).

**Estado:** Aceptable.

---

### 2.3 Hallazgos RECOMENDACION

#### REC-01: Documentar la semantica de colores en el archivo de gestion de riesgos

**Descripcion:** Se recomienda incluir en la documentacion del proyecto una tabla que defina la asociacion entre colores y estados de riesgo, para mantener consistencia si se realizan cambios futuros.

**Justificacion:** ISO 14971:2019 clausula 4.5 requiere que el fabricante mantenga un archivo de gestion de riesgos. Aunque MBR es una herramienta de soporte y no un dispositivo medico, es buena practica documentar las convenciones visuales que comunican el estado del riesgo.

**Accion sugerida:** Agregar al comentario inicial de `mbr.css` o a un documento de estilo la tabla de asociacion color-semantica.

**Prioridad:** Baja.

---

#### REC-02: Verificar contraste WCAG AA en combinaciones criticas

**Descripcion:** El diseno propone verificar el contraste segun WCAG AA (CA-9). Se recomienda documentar los ratios de contraste calculados para las combinaciones criticas:

| Combinacion | Ratio estimado | Cumple WCAG AA? |
|-------------|----------------|-----------------|
| Texto #1C1917 sobre #F5F4F1 | ~14:1 | Si |
| Texto #0E6B3A sobre #DDF0E4 | ~6:1 | Si |
| Texto #7A1F1F sobre #F8DCDC | ~7:1 | Si |
| Texto #7A5300 sobre #FBEFD3 | ~6:1 | Si |

**Nota:** Los ratios son estimaciones. Se recomienda validar con una herramienta de contraste durante la implementacion (CA-9, Tarea T-19).

**Clausula relacionada:** No aplica directamente a ISO 14971. Relacionado con IEC 62366 y buenas practicas de accesibilidad.

**Prioridad:** Baja.

---

#### REC-03: Considerar daltonismo en indicadores criticos

**Descripcion:** Los indicadores de riesgo aceptable/no aceptable dependen de la diferenciacion rojo/verde. Para usuarios con daltonismo, esta diferenciacion puede ser dificil.

**Estado actual:** El texto de los tags incluye el nivel de riesgo textual (ej: "Sin estimar", nivel de riesgo calculado), lo cual mitiga parcialmente el problema.

**Accion sugerida (futura):** Considerar agregar iconos o patrones adicionales a los tags para diferenciar estados sin depender solo del color (ej: icono de check para aceptable, X para no aceptable).

**Prioridad:** Baja (fuera del alcance actual, mejora futura).

---

## 3. Verificacion de Trazabilidad

### 3.1 Cadena de trazabilidad del modelo MBR

El prototipo MBR implementa la cadena de trazabilidad definida en ISO 14971:

```
Peligro (hazard)
    |
    v
Situacion peligrosa (hazardous situation)
    |
    v
Dano (harm)
    |
    v
Estimacion de riesgo (severity x probability)
    |
    v
Evaluacion contra criterios (verdict)
    |
    v
Medida de control (control measure)
    |
    v
Verificacion de efectividad
```

### 3.2 Impacto de los cambios visuales en la trazabilidad

**Verificacion:** Los cambios propuestos NO modifican:

1. **Modelo de datos:** No se alteran las entidades `RiskChain`, `Estimation`, `ControlMeasure`, ni sus relaciones en la base de datos.

2. **Logica de evaluacion:** Las clases `RiskAssessor`, `RiskMatrix`, `RiskEstimator` permanecen intactas.

3. **Visualizacion de la cadena:** Las vistas (`RisksView`, `AssessmentView`, `TraceabilityView`) mantienen la misma estructura de datos; solo cambian estilos CSS.

4. **Indicadores de estado:** Los tags y veredictos siguen mostrando la misma informacion (`acceptable()`, `riskLevel()`), solo con estilos mejorados.

**Conclusion:** La trazabilidad peligro - situacion peligrosa - dano - riesgo - control - verificacion **no se ve afectada** por los cambios visuales propuestos.

---

## 4. Verificacion de Indicadores de Riesgo

### 4.1 Clases CSS analizadas

| Clase | Archivo CSS (linea) | Proposito | Cambio propuesto |
|-------|---------------------|-----------|------------------|
| `.tag` | mbr.css:155-158 | Estado neutral/informativo | font-size: 11px -> 12px |
| `.tag-ok` | mbr.css:237-240 | Riesgo aceptable / con medida | font-size: 11px -> 12px |
| `.tag-bad` | mbr.css:177-180 | Riesgo no aceptable | font-size: 11px -> 12px |
| `.tag-warn` | mbr.css:241-244 | Advertencia / sin medida | font-size: 11px -> 12px |
| `.verdict-ok` | mbr.css:181 | Veredicto aceptable (grande) | Sin cambios |
| `.verdict-bad` | mbr.css:182 | Veredicto no aceptable (grande) | Sin cambios |

### 4.2 Uso en el codigo

**AssessmentView.java:**
```java
// Linea 173: Chip de estado en lista de cadenas
Label chip = e == null ? Ui.label("Sin estimar", "hint")
        : Ui.label(e.riskLevel(), e.acceptable() ? "tag" : "tag-bad");

// Linea 262: Veredicto principal
Label level = Ui.label(e.riskLevel(), e.acceptable() ? "verdict-ok" : "verdict-bad");
```

**TraceabilityView.java:**
```java
// Linea 168: Cobertura de requisitos
Label coverage = Ui.label(linked == 0 ? "Sin medida de control"
        : linked + (linked == 1 ? " medida" : " medidas"),
        linked == 0 ? "tag-warn" : "tag-ok");
```

### 4.3 Validacion

| Criterio | Estado | Observacion |
|----------|--------|-------------|
| Colores semanticos claros | OK | Verde=aceptable, Rojo=no aceptable, Amarillo=advertencia |
| Contraste suficiente | OK | Ratios estimados >= 4.5:1 (verificar en T-19) |
| Consistencia entre vistas | OK | Mismas clases usadas en AssessmentView y TraceabilityView |
| Texto legible (12px minimo) | Pendiente | Cambio de 11px a 12px en las tareas T-1, T-2 |

---

## 5. Verificacion de Criterios de Aceptacion de Riesgo

### 5.1 Implementacion en el modelo

Los criterios de aceptacion de riesgo estan implementados en `RiskMatrix.java`:

```java
// RiskMatrix.java:26
public record Criterion(String riskLevel, boolean acceptable) {}

// RiskMatrix.java:28-29
public record Verdict(SeverityLevel severity, ProbabilityLevel probability,
                      String riskLevel, boolean acceptable) {}
```

La evaluacion ocurre en:
```java
// RiskMatrix.java:113-126
public Verdict evaluate(int severity, double probability, String unit) {
    // ... validaciones ...
    Cell cell = cells.get(key(severity, p.value()));
    return new Verdict(s, p, cell.riskLevel(), criteria.get(cell.riskLevel()));
}
```

### 5.2 Impacto de los cambios visuales

**Ninguno.** Los cambios visuales no modifican:
- La estructura `Criterion`
- El metodo `evaluate()`
- La logica de decision `acceptable()`

Los indicadores visuales (`.verdict-ok`, `.verdict-bad`) simplemente reflejan el valor booleano `acceptable()` retornado por la matriz de riesgo.

---

## 6. Verificacion de Controles de Riesgo y su Verificacion

### 6.1 Estado actual

El prototipo MBR implementa:
- **Medidas de control:** Vinculadas a cadenas de riesgo en `AssessmentView` (tab "Control y verificacion")
- **Trazabilidad:** Matriz de trazabilidad en `TraceabilityView` que relaciona requisitos con medidas de control
- **Verificacion:** Campo de evidencia en las estimaciones y medidas

### 6.2 Impacto de los cambios visuales

**Ninguno.** Los cambios visuales propuestos no afectan:
- La estructura de datos de controles
- La relacion control - verificacion
- La visualizacion de la cadena de trazabilidad

Los cambios mejoran la **legibilidad** de la informacion de trazabilidad (estilos de tabla, hover en filas, estados vacios informativos) sin alterar su contenido.

---

## 7. Terminologia Normativa

### 7.1 Verificacion de conceptos ISO 14971

| Concepto ISO 14971 | Implementacion MBR | Estado |
|--------------------|-------------------|--------|
| Peligro (hazard) | `RiskChain.hazard` | OK |
| Situacion peligrosa | `RiskChain.situation` | OK |
| Dano (harm) | `RiskChain.harm` | OK |
| Severidad | `Estimation.severity`, `SeverityLevel` | OK |
| Probabilidad | `Estimation.pTotal`, `ProbabilityLevel` | OK |
| Riesgo | Combinacion severidad x probabilidad | OK |
| Criterio de aceptabilidad | `RiskMatrix.Criterion` | OK |
| Medida de control | `ControlMeasure` (via TraceabilityView) | OK |

### 7.2 Observacion

No se detectaron inconsistencias entre la terminologia usada en el codigo/interfaz y los conceptos de ISO 14971. Los cambios visuales propuestos no introducen nuevos terminos ni modifican los existentes.

---

## 8. Conclusion y Proximos Pasos

### 8.1 Conclusion

Los cambios de mejoras visuales propuestos en `docs/specs/mejoras-visuales.md` y `docs/design/mejoras-visuales.md` son **APROBADOS** desde la perspectiva normativa porque:

1. **No afectan la logica de gestion de riesgos:** Las clases `RiskAssessor`, `RiskMatrix`, `RiskEstimator` permanecen intactas.

2. **No alteran la trazabilidad:** La cadena peligro - situacion - dano - riesgo - control - verificacion se mantiene integra.

3. **Mejoran la usabilidad:** Mejor legibilidad, contraste y feedback visual facilitan el uso del sistema para gestion de riesgos.

4. **Mantienen la semantica de indicadores:** Los colores y estilos de tags/veredictos siguen comunicando el estado de aceptabilidad del riesgo de forma clara.

### 8.2 Proximos pasos

1. **Proceder con la implementacion** segun el plan de tareas del diseno tecnico.

2. **Verificar contraste WCAG AA** (Tarea T-19) con herramienta de medicion durante la implementacion.

3. **Documentar la asociacion color-semantica** (REC-01) en el archivo CSS o documentacion del proyecto.

4. **Considerar para futuras versiones** mejoras de accesibilidad para daltonismo (REC-03).

---

## 9. Declaracion de Limitaciones

Este informe se basa en la revision de los documentos de especificacion, diseno y codigo fuente disponibles. Las clausulas de ISO 14971:2019 citadas reflejan mi conocimiento del estandar, pero no constituyen una auditoria formal de cumplimiento. Se recomienda validacion por personal certificado si el prototipo evoluciona hacia un producto comercial.

---

**Fin del informe de revision normativa.**
