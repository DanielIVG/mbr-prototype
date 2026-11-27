# Diseno Tecnico: Mejoras Visuales de la Interfaz

**Producto:** MBR - Prototipo de gestion de riesgos basado en modelos para dispositivos medicos
**Version:** 1.0
**Fecha:** 2026-11-27
**Responsable:** Arquitecto
**Especificacion de entrada:** `docs/specs/mejoras-visuales.md`

---

## 1. Analisis del Estado Actual

### 1.1 Fortalezas del Sistema de Diseno Actual

El sistema de diseno actual en `src/main/resources/css/mbr.css` presenta las siguientes fortalezas:

1. **Paleta de colores bien definida:** El color primario teal (#15616D) y el fondo (#F5F4F1) estan aplicados consistentemente.

2. **Clases CSS reutilizables:** Existen clases bien nombradas para componentes comunes:
   - Tipografia: `.page-title`, `.form-title`, `.form-title-sm`, `.section-title`, `.field-label`, `.hint`, `.table-head`
   - Botones: `.btn-primary`, `.btn-secondary`, `.btn-link`, `.btn-tab`, `.btn-tab-active`
   - Tarjetas: `.card`, `.card-head`, `.card-row`, `.card-row-last`
   - Listas: `.list-row`, `.list-row-selected`, `.row-title`
   - Tags: `.tag`, `.tag-ok`, `.tag-bad`, `.tag-warn`

3. **Estados hover funcionales en componentes principales:** Los botones, tiles y combo-boxes tienen estados hover definidos.

4. **Scroll personalizado:** La clase `.plain-scroll` proporciona scrollbars sutiles y no intrusivas.

5. **Clase utilitaria Ui.java bien estructurada:** Proporciona metodos para crear componentes consistentes (`label()`, `button()`, `icon()`, `topBar()`, `show()`, `confirm()`).

### 1.2 Gaps Identificados Respecto a los Requisitos

| Gap | Requisito Afectado | Estado Actual | Accion Requerida |
|-----|-------------------|---------------|------------------|
| G-1 | REQ-VIS-020 | `.table-head` usa 11px | Ajustar a 12px minimo |
| G-2 | REQ-VIS-005 | `.list-row:hover` existe pero `.card-row:hover` no | Agregar hover a filas de tarjeta |
| G-3 | REQ-VIS-006 | Focus visible solo en algunos controles | Estandarizar borde teal 2px en focus |
| G-4 | REQ-VIS-012 | TableView sin estilos de hover | Agregar estilos para filas de tabla |
| G-5 | REQ-VIS-015 | `.nav-item-active` existe pero sin transicion | Ya cumple, verificar contraste |
| G-6 | REQ-VIS-008 | Mensajes "Cargando..." en texto plano | Mantener simple, ya funcional |
| G-7 | REQ-VIS-010 | Tarjetas sin sombra | Evaluar agregar box-shadow sutil |
| G-8 | REQ-VIS-018 | Estados vacios sin iconos | Enriquecer con iconos SVG |
| G-9 | REQ-VIS-013 | Tags semanticos incompletos | `.tag-ok`, `.tag-bad`, `.tag-warn` existen; `.tag` es neutral |
| G-10 | REQ-VIS-016 | Solo 4 iconos en Ui.java | Agregar iconos adicionales |

---

## 2. Decisiones de Diseno

### 2.1 Estrategia de Retrocompatibilidad

**Decision D-1:** Modificar clases CSS existentes de forma aditiva, sin eliminar propiedades funcionales.

**Justificacion:** Las vistas existentes (20+) ya utilizan las clases actuales. Cambios destructivos requeririan auditar y modificar todas las vistas.

**Implementacion:**
- Agregar propiedades nuevas a clases existentes (hover, focus, transiciones).
- Crear clases nuevas solo cuando sea necesario extender funcionalidad.
- No renombrar clases existentes.

### 2.2 Uso de CSS vs Codigo Java

| Mejora | Implementacion | Justificacion |
|--------|---------------|---------------|
| Estados hover/focus | CSS | Son estados del selector, no requieren logica |
| Tamano minimo de fuente | CSS | Propiedad declarativa |
| Hover en TableView | CSS | JavaFX soporta `.table-row:hover` |
| Box-shadow en tarjetas | CSS | Propiedad visual pura |
| Iconos SVG adicionales | Java (Ui.java) | Se crean programaticamente con `SVGPath` |
| Estados vacios con icono | Java (Vistas) | Requieren estructura de nodos |
| Texto wrappable | Java (Vistas) | `setWrapText(true)` es metodo de Label |

**Decision D-2:** Preferir CSS para propiedades visuales y Java para estructura de componentes.

### 2.3 Convenciones de Nombrado de Clases CSS

**Convencion existente (mantener):**
- Componentes: `{componente}` - ej: `.card`, `.tile`, `.button`
- Variantes: `{componente}-{variante}` - ej: `.btn-primary`, `.tag-ok`
- Estados: `{componente}-{estado}` - ej: `.list-row-selected`, `.nav-item-active`
- Elementos: `{componente}-{elemento}` - ej: `.card-head`, `.card-row`

**Nuevas clases propuestas:**
- `.card-row-hover` (si se requiere clase explicita, aunque preferir `:hover`)
- `.table-row` (para estilos de fila de TableView)
- `.icon-lg` (iconos grandes para estados vacios)

---

## 3. Cambios Especificos en CSS

### 3.1 Archivo: `src/main/resources/css/mbr.css`

#### 3.1.1 Ajuste de Tamano Minimo de Fuente (REQ-VIS-020)

**Linea actual (41):**
```css
.table-head { -fx-text-fill: #6B6660; -fx-font-size: 11px; -fx-font-weight: 600; }
```

**Cambio:**
```css
.table-head { -fx-text-fill: #6B6660; -fx-font-size: 12px; -fx-font-weight: 600; }
```

#### 3.1.2 Hover en Filas de Tarjeta (REQ-VIS-005, REQ-VIS-011)

**Agregar despues de `.card-row-last` (linea 53):**
```css
.card-row:hover { -fx-background-color: #FAF9F7; }
```

#### 3.1.3 Estilos para TableView (REQ-VIS-012, REQ-VIS-007)

**Agregar al final del archivo (seccion nueva):**
```css
/* ---------- Tablas (TableView) ---------- */
.table-view {
    -fx-background-color: transparent;
    -fx-border-color: #E0DED8;
    -fx-border-radius: 8;
}
.table-view .column-header {
    -fx-background-color: #FAF9F7;
    -fx-border-color: transparent transparent #E0DED8 transparent;
    -fx-padding: 8 12 8 12;
}
.table-view .column-header .label {
    -fx-text-fill: #6B6660;
    -fx-font-size: 12px;
    -fx-font-weight: 600;
}
.table-view .table-row-cell {
    -fx-background-color: #FFFFFF;
    -fx-border-color: transparent transparent #EFEDE8 transparent;
    -fx-padding: 10 12 10 12;
}
.table-view .table-row-cell:hover {
    -fx-background-color: #F1F7F8;
}
.table-view .table-row-cell:selected {
    -fx-background-color: #E6F0F1;
}
.table-view .table-row-cell:selected .text {
    -fx-fill: #0E434B;
}
.table-view .table-row-cell .text {
    -fx-fill: #1C1917;
}
.table-view .placeholder .label {
    -fx-text-fill: #8A857E;
}
```

#### 3.1.4 Sombra Sutil en Tarjetas (REQ-VIS-010)

**Decision D-3:** Agregar sombra sutil opcional. Mantener estilo flat como default.

**Modificar `.card` (linea 45-50):**
```css
.card {
    -fx-background-color: #FFFFFF;
    -fx-border-color: #E0DED8;
    -fx-border-radius: 10;
    -fx-background-radius: 10;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);
}
```

**Alternativa descartada:** Usar clase separada `.card-shadow`. Razon: agrega complejidad sin beneficio claro; la sombra sutil no afecta la legibilidad.

#### 3.1.5 Focus Visible Consistente (REQ-VIS-006)

**Agregar seccion de focus global despues de `.root` (linea 10):**
```css
/* ---------- Focus global ---------- */
.button:focused, .text-field:focused, .text-area:focused,
.combo-box:focused, .check-box:focused .box,
.list-row:focused, .tile:focused, .nav-item:focused {
    -fx-border-color: #15616D;
    -fx-border-width: 2;
}
```

**Nota:** Algunas clases ya tienen `:focused` definido (`.btn-primary:focused`, `.tile:focused`). Se mantienen para especificidad.

#### 3.1.6 Estados Disabled Mejorados (REQ-VIS-007)

**El estado actual (linea 96) ya es adecuado:**
```css
.button:disabled { -fx-opacity: 0.5; -fx-cursor: default; }
```

**Agregar para otros controles:**
```css
.text-field:disabled, .text-area:disabled, .combo-box:disabled {
    -fx-opacity: 0.5;
    -fx-background-color: #F5F4F1;
}
.list-row:disabled, .tile:disabled {
    -fx-opacity: 0.6;
    -fx-cursor: default;
}
.list-row:disabled:hover, .tile:disabled:hover {
    -fx-border-color: #E0DED8;
}
```

#### 3.1.7 Ajuste de Tamano de Tags (REQ-VIS-013)

**Estado actual (lineas 155-158, 177-180, 237-244):**
Los tags usan `font-size: 11px`. Ajustar a 12px para cumplir REQ-VIS-020.

**Cambios:**
```css
.tag { ... -fx-font-size: 12px; }
.tag-bad { ... -fx-font-size: 12px; }
.tag-ok { ... -fx-font-size: 12px; }
.tag-warn { ... -fx-font-size: 12px; }
```

#### 3.1.8 Chip de Estado Neutral (REQ-VIS-013)

**La clase `.tag` ya funciona como neutral (teal claro).** No se requiere cambio adicional.

#### 3.1.9 Hover en Items de Navegacion (REQ-VIS-015)

**Estado actual (lineas 173-174):**
```css
.nav-item-on { -fx-cursor: hand; }
.nav-item-on:hover { -fx-background-color: #F0EEEA; }
```

**Verificacion:** Cumple con el requisito. El estado activo (`.nav-item-active`) ya tiene fondo teal claro (#E6F0F1) y texto oscuro.

### 3.2 Resumen de Nuevas Clases CSS

| Clase | Proposito | Prioridad |
|-------|-----------|-----------|
| `.table-view` (y descendientes) | Estilos de tabla | Importante |
| `.icon-lg` | Iconos grandes (64px) para estados vacios | Deseable |

### 3.3 Variables/Constantes CSS

**Decision D-4:** No usar variables CSS nativas (no soportadas en JavaFX 17). Mantener valores hardcodeados con comentarios de referencia.

**Referencia de colores (comentario al inicio del archivo):**
```css
/*
 * Paleta de colores MBR:
 * Primario: #15616D (teal)
 * Hover primario: #0E434B
 * Fondo pagina: #F5F4F1
 * Fondo tarjeta: #FFFFFF
 * Texto principal: #1C1917
 * Texto secundario: #6B6660
 * Texto hint: #8A857E
 * Borde tarjeta: #E0DED8
 * Borde campo: #CFCBC3
 * Separador sutil: #EFEDE8
 */
```

---

## 4. Cambios en Ui.java

### 4.1 Archivo: `src/main/java/org/example/view/Ui.java`

#### 4.1.1 Iconos SVG Adicionales (REQ-VIS-016, REQ-VIS-017, REQ-VIS-018)

**Agregar constantes de iconos despues de ICON_BLOCKS (linea 26):**

```java
// Iconos existentes
static final String ICON_PLUS = "M12 5v14 M5 12h14";
static final String ICON_GRID = "M3 3h18v18H3z M9 3v18 M3 9h18";
static final String ICON_DOC = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z M14 3v5h5";
static final String ICON_BLOCKS = "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M17.5 14v7 M14 17.5h7";

// Iconos nuevos para estados vacios y acciones
static final String ICON_INBOX = "M22 12h-6l-2 3H10l-2-3H2 M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z";
static final String ICON_ALERT_TRIANGLE = "M12 9v4 M12 17h.01 M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z";
static final String ICON_CHECK_CIRCLE = "M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3";
static final String ICON_X_CIRCLE = "M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z M15 9l-6 6 M9 9l6 6";
static final String ICON_EDIT = "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
static final String ICON_TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2";
static final String ICON_LINK = "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71 M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71";
static final String ICON_CLIPBOARD = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M15 2H9a1 1 0 0 0-1 1v2a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1z";
static final String ICON_LAYERS = "M12 2L2 7l10 5 10-5-10-5z M2 17l10 5 10-5 M2 12l10 5 10-5";
static final String ICON_SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
```

#### 4.1.2 Metodo para Iconos Grandes (REQ-VIS-018)

**Agregar metodo despues de `icon()` (linea 64):**

```java
/** Icono grande para estados vacios (64x64px). */
static Node emptyIcon(String content, Color stroke) {
    SVGPath path = icon(content, stroke);
    path.setScaleX(2.5);
    path.setScaleY(2.5);
    HBox box = new HBox(path);
    box.getStyleClass().add("empty-icon-box");
    box.setAlignment(Pos.CENTER);
    box.setMinSize(64, 64);
    box.setPrefSize(64, 64);
    box.setMaxSize(64, 64);
    return box;
}
```

#### 4.1.3 Metodo para Estados Vacios Enriquecidos (REQ-VIS-018, HU-VIS-004)

**Agregar metodo:**

```java
/** Estado vacio con icono, titulo y mensaje explicativo. */
static VBox emptyState(String iconPath, String title, String message) {
    VBox box = new VBox(16);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(32));

    Node icon = emptyIcon(iconPath, Color.web("#15616D"));
    Label titleLabel = label(title, "empty-title");
    Label messageLabel = label(message, "hint");
    messageLabel.setWrapText(true);
    messageLabel.setMaxWidth(400);
    messageLabel.setAlignment(Pos.CENTER);

    box.getChildren().addAll(icon, titleLabel, messageLabel);
    return box;
}
```

#### 4.1.4 Metodo Utilitario para Labels con Wrap

**Agregar metodo (ya existe como metodo privado en varias vistas, centralizarlo):**

```java
/** Label con texto que hace wrap automaticamente. */
static Label wrappedLabel(String text, String... styleClasses) {
    Label label = new Label(text);
    label.getStyleClass().addAll(styleClasses);
    label.setWrapText(true);
    return label;
}
```

---

## 5. Cambios en Vistas

### 5.1 Lista de Vistas que Requieren Modificacion

| Vista | Archivo | Tipo de Cambio | Prioridad |
|-------|---------|----------------|-----------|
| HomeView | `HomeView.java` | Estados vacios enriquecidos, hover en filas | Importante |
| WorkspaceView | `WorkspaceView.java` | Verificar navegacion activa | Verificar |
| StructureView | `StructureView.java` | Estados vacios, texto wrap | Importante |
| BehaviorView | `BehaviorView.java` | Estados vacios | Importante |
| RisksView | `RisksView.java` | Estados vacios enriquecidos | Importante |
| AssessmentView | `AssessmentView.java` | Estados vacios, texto wrap | Importante |
| FtaView | `FtaView.java` | Estados vacios | Deseable |
| RpnDashboardView | `RpnDashboardView.java` | Estados vacios, estilos tabla | Importante |
| TraceabilityView | `TraceabilityView.java` | Estados vacios, estilos tabla | Importante |
| CriteriaView | `CriteriaView.java` | Estados vacios | Deseable |
| ModelGraphView | `ModelGraphView.java` | Estados vacios | Deseable |

### 5.2 Cambios por Vista

#### 5.2.1 HomeView.java

**Cambios requeridos:**
1. Reemplazar estado vacio de lista de dispositivos con `Ui.emptyState()`.
2. Verificar que las filas de tarjeta tengan hover (ya aplicado via CSS `.card-row:hover`).

**Ubicacion:** Lineas 167-174 (metodo `showDevices`)

**Antes:**
```java
if (devices.isEmpty()) {
    VBox empty = new VBox(4,
            Ui.label("Todavia no hay dispositivos.", "row-title"),
            Ui.label("Usa <<Crear dispositivo>> para registrar el primero.", "muted"));
    empty.getStyleClass().add("card-row-last");
    listBody.getChildren().add(empty);
    return;
}
```

**Despues:**
```java
if (devices.isEmpty()) {
    VBox empty = Ui.emptyState(Ui.ICON_INBOX,
            "Todavia no hay dispositivos",
            "Usa <<Crear dispositivo>> para registrar el primer dispositivo medico.");
    listBody.getChildren().add(empty);
    return;
}
```

#### 5.2.2 RisksView.java

**Cambios requeridos:**
1. Estado vacio enriquecido para lista de cadenas de riesgo.

**Ubicacion:** Lineas 88-95

#### 5.2.3 AssessmentView.java

**Cambios requeridos:**
1. Estado vacio cuando no hay cadenas para evaluar.
2. Asegurar que labels largos tengan wrap (ya implementado en muchos casos).

**Ubicacion:** Lineas 147-151

#### 5.2.4 RpnDashboardView.java

**Cambios requeridos:**
1. Estado vacio enriquecido.
2. Estilos de TableView se aplican automaticamente via CSS.

**Ubicacion:** Lineas 68-73

#### 5.2.5 TraceabilityView.java

**Cambios requeridos:**
1. Estado vacio enriquecido para lista de requisitos.
2. Estilos de TableView automaticos.

**Ubicacion:** Lineas 117-120

### 5.3 Patron de Cambio para Estados Vacios

Todas las vistas que muestran listas o tablas seguiran este patron:

```java
// Antes (texto plano)
if (items.isEmpty()) {
    Label empty = Ui.label("Mensaje explicativo", "hint");
    empty.setWrapText(true);
    container.getChildren().add(empty);
}

// Despues (estado vacio enriquecido)
if (items.isEmpty()) {
    VBox empty = Ui.emptyState(
        Ui.ICON_INBOX,  // o ICON_CLIPBOARD, ICON_LAYERS segun contexto
        "Titulo descriptivo",
        "Mensaje explicativo de como proceder.");
    container.getChildren().add(empty);
}
```

---

## 6. Plan de Implementacion

### 6.1 Orden de Implementacion

Las tareas estan ordenadas por prioridad y dependencias.

#### Fase 1: Cambios Criticos (CSS base)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-1 | Ajustar `.table-head` de 11px a 12px | mbr.css | Ninguna | Bajo |
| T-2 | Ajustar `.tag`, `.tag-ok`, `.tag-bad`, `.tag-warn` a 12px | mbr.css | Ninguna | Bajo |
| T-3 | Agregar comentario de paleta de colores | mbr.css | Ninguna | Bajo |

#### Fase 2: Estados Interactivos (CSS)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-4 | Agregar `.card-row:hover` | mbr.css | Ninguna | Bajo |
| T-5 | Agregar seccion de focus global | mbr.css | Ninguna | Medio |
| T-6 | Agregar estados disabled para campos y listas | mbr.css | Ninguna | Bajo |

#### Fase 3: Estilos de Tabla (CSS)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-7 | Agregar seccion completa de `.table-view` | mbr.css | Ninguna | Medio |

#### Fase 4: Mejoras Visuales (CSS)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-8 | Agregar box-shadow sutil a `.card` | mbr.css | Ninguna | Bajo |

#### Fase 5: Iconografia (Java)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-9 | Agregar constantes de iconos nuevos | Ui.java | Ninguna | Bajo |
| T-10 | Agregar metodo `emptyIcon()` | Ui.java | T-9 | Bajo |
| T-11 | Agregar metodo `emptyState()` | Ui.java | T-10 | Bajo |
| T-12 | Agregar metodo `wrappedLabel()` | Ui.java | Ninguna | Bajo |

#### Fase 6: Estados Vacios en Vistas (Java)

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-13 | Actualizar estado vacio en HomeView | HomeView.java | T-11 | Bajo |
| T-14 | Actualizar estado vacio en RisksView | RisksView.java | T-11 | Bajo |
| T-15 | Actualizar estado vacio en AssessmentView | AssessmentView.java | T-11 | Bajo |
| T-16 | Actualizar estado vacio en RpnDashboardView | RpnDashboardView.java | T-11 | Bajo |
| T-17 | Actualizar estado vacio en TraceabilityView | TraceabilityView.java | T-11 | Bajo |
| T-18 | Actualizar estado vacio en StructureView | StructureView.java | T-11 | Bajo |

#### Fase 7: Verificacion y Ajustes

| Tarea | Descripcion | Archivo | Dependencias | Riesgo |
|-------|-------------|---------|--------------|--------|
| T-19 | Verificar contraste WCAG AA en combinaciones criticas | N/A | T-1 a T-8 | Bajo |
| T-20 | Prueba visual de navegacion por teclado (focus) | N/A | T-5 | Bajo |
| T-21 | Verificar hover en todas las filas de tabla | N/A | T-7 | Bajo |

### 6.2 Diagrama de Dependencias

```
Fase 1: T-1, T-2, T-3 (independientes)
           |
           v
Fase 2: T-4, T-5, T-6 (independientes entre si)
           |
           v
Fase 3: T-7 (independiente)
           |
           v
Fase 4: T-8 (independiente)
           |
           v
Fase 5: T-9 -> T-10 -> T-11
         T-12 (independiente)
           |
           v
Fase 6: T-13, T-14, T-15, T-16, T-17, T-18 (dependen de T-11)
           |
           v
Fase 7: T-19, T-20, T-21 (verificacion)
```

### 6.3 Estimacion de Esfuerzo

| Fase | Tareas | Esfuerzo Estimado |
|------|--------|-------------------|
| Fase 1 | T-1 a T-3 | 15 minutos |
| Fase 2 | T-4 a T-6 | 20 minutos |
| Fase 3 | T-7 | 30 minutos |
| Fase 4 | T-8 | 5 minutos |
| Fase 5 | T-9 a T-12 | 30 minutos |
| Fase 6 | T-13 a T-18 | 45 minutos |
| Fase 7 | T-19 a T-21 | 30 minutos |
| **Total** | 21 tareas | ~3 horas |

---

## 7. Riesgos Tecnicos

### 7.1 Riesgos Identificados

| ID | Riesgo | Probabilidad | Impacto | Mitigacion |
|----|--------|--------------|---------|------------|
| R-1 | Estilos de TableView no se aplican correctamente en JavaFX | Media | Alto | Probar en T-7 antes de continuar; usar `setStyle()` inline si falla |
| R-2 | Focus visible interfiere con layout (border-width: 2 cambia tamano) | Baja | Medio | Usar `border-insets` negativos o `outline` si disponible |
| R-3 | Box-shadow no soportado en version de JavaFX | Baja | Bajo | La propiedad `-fx-effect: dropshadow()` es soportada desde JavaFX 8 |
| R-4 | Iconos SVG no renderizan correctamente | Baja | Bajo | Los paths son estandar de Feather Icons; probar visualmente |

### 7.2 Plan de Contingencia

**Para R-1 (TableView):** Si los estilos CSS no se aplican, implementar `CellFactory` personalizado que aplique estilos via `setStyle()`. Esto es mas trabajo pero funciona garantizado.

**Para R-2 (Focus):** Usar padding interno para compensar el borde, o limitar el estilo de focus a controles especificos en lugar de globales.

---

## 8. Alternativas Descartadas

### 8.1 Usar Variables CSS Nativas

**Propuesta:** Definir variables CSS (`--color-primary: #15616D`) para facilitar temas.

**Razon de descarte:** JavaFX 17 no soporta variables CSS nativas. Implementar un preprocesador seria sobreingenieria para un prototipo academico.

### 8.2 Implementar Modo Oscuro

**Propuesta:** Agregar archivo `mbr-dark.css` con tema oscuro.

**Razon de descarte:** Fuera del alcance segun especificacion. El esfuerzo de duplicar y mantener dos archivos CSS no se justifica para el prototipo.

### 8.3 Usar ControlsFX para Componentes Enriquecidos

**Propuesta:** Usar `Notifications`, `Dialogs` y otros componentes de ControlsFX.

**Razon de descarte:** Agrega dependencia externa. Los componentes nativos de JavaFX son suficientes para el alcance del prototipo. La especificacion no requiere tooltips enriquecidos ni spinners animados.

### 8.4 Reemplazar TableView con VBox Personalizado

**Propuesta:** Construir tablas manualmente con VBox/HBox para control total de estilos.

**Razon de descarte:** TableView ya proporciona funcionalidad de ordenacion, seleccion y scroll. Los estilos CSS basicos son suficientes para las necesidades del prototipo.

### 8.5 Usar Clases CSS Separadas para Cada Estado

**Propuesta:** Crear `.btn-primary-hover`, `.btn-primary-focus`, etc.

**Razon de descarte:** Los selectores de pseudo-clase (`:hover`, `:focused`) son la forma idiomatica en CSS y funcionan en JavaFX. Clases separadas agregarian complejidad en el codigo Java.

---

## 9. Respuestas a Preguntas Abiertas de la Especificacion

| Pregunta | Respuesta/Decision |
|----------|-------------------|
| 1. Iconografia expandida | Agregar 10 iconos adicionales inline SVG (Feather-style). No se integra libreria externa. |
| 2. Sombras en tarjetas | Se agrega sombra sutil (`dropshadow`). Estilo flat se mantiene como predominante. |
| 3. Tablas nativas JavaFX | Se personalizan parcialmente via CSS. No se reemplazan con custom. |
| 4. Tooltips enriquecidos | Se mantienen tooltips nativos simples. No se requieren para el prototipo. |
| 5. Indicadores de progreso | Se mantiene texto "Cargando...". No se agregan spinners animados. |
| 6. Responsividad real | Se verifica 1024x768. No se implementan layouts adaptativos complejos. |
| 7. Modo oscuro | No se implementa. No se estructura CSS para temas futuros. |
| 8. Densidad de informacion | Se mantiene diseno actual (espaciado generoso). |
| 9. Accesibilidad avanzada | Solo contraste y tamano de fuente. No se implementa ARIA. |
| 10. Prototipo vs producto | Priorizar velocidad de implementacion con calidad aceptable para demostracion academica. |

---

## 10. Anexos

### 10.1 Checklist de Criterios de Aceptacion

Referencia cruzada con los CA de la especificacion:

| CA | Tarea(s) Relacionada(s) | Como Verificar |
|----|------------------------|----------------|
| CA-1 | T-1, T-2, T-3 | Revision de mbr.css |
| CA-2 | T-4, T-6 | Hover en cada componente |
| CA-3 | T-5 | Navegacion Tab por formularios |
| CA-4 | (ya cumple) | Provocar errores en formularios |
| CA-5 | T-13 a T-18 | Crear dispositivo nuevo, revisar secciones |
| CA-6 | T-2 | Revisar AssessmentView, TraceabilityView |
| CA-7 | (ya cumple) | Revisar AssessmentView, FtaView |
| CA-8 | T-7 | Hover en RpnDashboardView, TraceabilityView |
| CA-9 | T-19 | WebAIM Contrast Checker |
| CA-10 | T-1, T-2 | Revision de mbr.css |
| CA-11 | (ya cumple) | Provocar carga de datos |
| CA-12 | (ya cumple) | Navegar 9 secciones |
| CA-13 | (ya cumple) | Revision visual de botones |
| CA-14 | T-8 | Revision de tarjetas |
| CA-15 | T-4 | Revision de listas |
| CA-16 | T-9 | Revision visual de iconos |
| CA-17 | (ya cumple) | Scroll en listas largas |
| CA-18 | T-21 | Redimensionar a 1024x768 |
| CA-19 | T-12, Fase 6 | Revisar tablas con texto largo |
| CA-20 | (ya cumple) | Interaccion general |

### 10.2 Archivos Modificados (Resumen)

| Archivo | Lineas Afectadas (aprox) | Tipo de Cambio |
|---------|-------------------------|----------------|
| `src/main/resources/css/mbr.css` | +60, ~10 modificadas | CSS |
| `src/main/java/org/example/view/Ui.java` | +50 | Java |
| `src/main/java/org/example/view/HomeView.java` | ~8 | Java |
| `src/main/java/org/example/view/RisksView.java` | ~6 | Java |
| `src/main/java/org/example/view/AssessmentView.java` | ~6 | Java |
| `src/main/java/org/example/view/RpnDashboardView.java` | ~6 | Java |
| `src/main/java/org/example/view/TraceabilityView.java` | ~6 | Java |
| `src/main/java/org/example/view/StructureView.java` | ~6 | Java |

---

**Fin del documento de diseno.**
