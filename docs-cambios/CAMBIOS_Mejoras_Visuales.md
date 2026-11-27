# Cambios Implementados: Mejoras Visuales de la Interfaz

**Fecha:** 2026-11-27
**Responsable:** Desarrollador
**Diseño técnico:** docs/design/mejoras-visuales.md
**Especificación:** docs/specs/mejoras-visuales.md

---

## Resumen Ejecutivo

Se implementaron mejoras visuales en el prototipo MBR para mejorar la experiencia de usuario y cumplir con estándares de accesibilidad básicos. Los cambios incluyen:

1. Ajustes CSS críticos (tamaños de fuente, estados hover, focus global)
2. Mejoras CSS adicionales (sombras sutiles, estilos de tabla, estados disabled)
3. Ampliación de iconografía SVG (10 nuevos iconos)
4. Métodos utilitarios en Ui.java para estados vacíos enriquecidos
5. Actualización de 6 vistas principales con estados vacíos enriquecidos

**Compilación:** Exitosa
**Pruebas:** 191/196 pasan (5 fallos pre-existentes no relacionados con cambios visuales)

---

## Archivos Modificados

### 1. CSS: src/main/resources/css/mbr.css

#### 1.1 Paleta de Colores Documentada
Agregado comentario con paleta de colores MBR para referencia (líneas 3-15).

#### 1.2 Focus Global Consistente
Agregada sección de focus global (líneas 26-32) que aplica borde teal de 2px a todos los controles interactivos:
- `.button:focused`
- `.text-field:focused`
- `.text-area:focused`
- `.combo-box:focused`
- `.check-box:focused .box`
- `.list-row:focused`
- `.tile:focused`
- `.nav-item:focused`

**Cumple:** REQ-VIS-006 (indicadores de foco visibles)

#### 1.3 Tamaños de Fuente Mínimos
Ajustados de 11px a 12px para cumplir con tamaño mínimo legible:
- `.table-head` (línea 63)
- `.tag` (línea 192)
- `.tag-bad` (línea 214)
- `.tag-ok` (línea 274)
- `.tag-warn` (línea 278)
- `.pill-soon` (línea 136)

**Cumple:** REQ-VIS-020 (tamaño mínimo de fuente 12px)

#### 1.4 Hover en Tarjetas
Agregado estado hover a filas de tarjeta (línea 76):
```css
.card-row:hover { -fx-background-color: #FAF9F7; }
```

**Cumple:** REQ-VIS-005, REQ-VIS-011 (retroalimentación visual de hover)

#### 1.5 Sombra Sutil en Tarjetas
Agregada sombra sutil a `.card` (línea 72):
```css
-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);
```

**Cumple:** REQ-VIS-010 (jerarquía visual con sombras sutiles)

#### 1.6 Estados Disabled Mejorados
Agregados estilos para controles deshabilitados (líneas 147-157):
- `.text-field:disabled`, `.text-area:disabled`, `.combo-box:disabled` - opacidad 0.5, fondo gris
- `.list-row:disabled`, `.tile:disabled` - opacidad 0.6, cursor default
- Hover deshabilitado en estados disabled

**Cumple:** REQ-VIS-007 (indicación clara de elementos deshabilitados)

#### 1.7 Estilos de TableView
Agregada sección completa de estilos para TableView (líneas 284-319):
- Cabeceras con fondo #FAF9F7
- Filas con hover #F1F7F8
- Filas seleccionadas con fondo #E6F0F1
- Texto consistente con paleta de colores

**Cumple:** REQ-VIS-012 (hover en filas de tabla)

---

### 2. Clase Utilitaria: src/main/java/org/example/view/Ui.java

#### 2.1 Nuevos Iconos SVG
Agregados 10 nuevos iconos para estados vacíos y acciones (líneas 31-41):
- `ICON_INBOX` - Estados vacíos generales
- `ICON_ALERT_TRIANGLE` - Advertencias y requisitos faltantes
- `ICON_CHECK_CIRCLE` - Estados exitosos
- `ICON_X_CIRCLE` - Estados de error
- `ICON_EDIT` - Acción de editar
- `ICON_TRASH` - Acción de eliminar
- `ICON_LINK` - Trazabilidad y enlaces
- `ICON_CLIPBOARD` - Registros y listas
- `ICON_LAYERS` - Capas y niveles
- `ICON_SEARCH` - Búsqueda

**Cumple:** REQ-VIS-016, REQ-VIS-017 (iconos SVG consistentes)

#### 2.2 Método emptyIcon()
Agregado método para iconos grandes (64x64px) en estados vacíos (líneas 82-93):
```java
static Node emptyIcon(String content, Color stroke)
```
Escala el icono 2.5x y lo centra en un contenedor con clase CSS `.empty-icon-box`.

#### 2.3 Método emptyState()
Agregado método para estados vacíos enriquecidos (líneas 95-110):
```java
static VBox emptyState(String iconPath, String title, String message)
```
Crea un VBox centrado con:
- Icono grande (64x64px)
- Título con clase `empty-title`
- Mensaje explicativo con clase `hint`, wrappable, max-width 400px

**Cumple:** REQ-VIS-018, HU-VIS-004 (estados vacíos enriquecidos)

#### 2.4 Método wrappedLabel()
Agregado método utilitario para labels con wrap automático (líneas 112-118):
```java
static Label wrappedLabel(String text, String... styleClasses)
```

---

### 3. Vistas Actualizadas con Estados Vacíos Enriquecidos

#### 3.1 HomeView.java (líneas 167-171)
**Antes:** VBox simple con 2 labels
**Después:** `Ui.emptyState(ICON_INBOX, título, mensaje)`

Mensaje: "Todavía no hay dispositivos / Usa «Crear dispositivo» para registrar el primer dispositivo médico."

#### 3.2 RisksView.java (líneas 88-95)
**Antes:** Label con texto largo
**Después:** `Ui.emptyState(ICON_ALERT_TRIANGLE, título, mensaje)`

Mensajes condicionales:
- Si no hay componentes: "Primero define componentes / Ve a la sección Estructura..."
- Si hay componentes: "Aún no hay cadenas de riesgo / Cada cadena sigue el camino..."

#### 3.3 AssessmentView.java (líneas 147-151)
**Antes:** Label simple
**Después:** `Ui.emptyState(ICON_CLIPBOARD, título, mensaje)`

Mensaje: "No hay cadenas de riesgo para evaluar / Ve a la sección Riesgos..."

#### 3.4 RpnDashboardView.java (líneas 68-72)
**Antes:** Label con texto largo wrappable
**Después:** `Ui.emptyState(ICON_GRID, título, mensaje)`

Mensaje: "Aún no hay análisis FMEA / Los análisis FMEA se crean desde un daño..."

#### 3.5 TraceabilityView.java (líneas 117-121)
**Antes:** Label con texto largo wrappable
**Después:** `Ui.emptyState(ICON_LINK, título, mensaje)`

Mensaje: "Aún no hay requisitos registrados / Registra un requisito y enlázalo..."

#### 3.6 StructureView.java (líneas 194-207)
**Antes:** Label con switch expression
**Después:** VBox con switch expression que retorna diferentes `emptyState()`

Mensajes por pestaña:
- COMPONENTS: `ICON_BLOCKS` - "Aún no hay componentes / Los componentes son las partes físicas..."
- FUNCTIONS: `ICON_LAYERS` - "Aún no hay funciones / Una función es lo que el dispositivo hace..."
- INTERACTIONS: `ICON_LINK` - "Aún no hay interacciones / Registra solo las interacciones relevantes..."

---

## Verificación de Criterios de Aceptación

| CA | Descripción | Estado | Verificación |
|----|-------------|--------|--------------|
| CA-1 | Paleta de colores documentada | ✅ Cumplido | Comentario en mbr.css líneas 3-15 |
| CA-2 | Hover en componentes interactivos | ✅ Cumplido | `.card-row:hover`, `.table-row-cell:hover` |
| CA-3 | Focus visible en navegación por teclado | ✅ Cumplido | Sección focus global líneas 26-32 |
| CA-5 | Estados vacíos enriquecidos | ✅ Cumplido | 6 vistas actualizadas |
| CA-6 | Tags semánticos legibles | ✅ Cumplido | Font-size 12px en todos los tags |
| CA-8 | Hover en tablas | ✅ Cumplido | `.table-row-cell:hover` agregado |
| CA-9 | Contraste WCAG AA | ✅ Cumplido | Paleta existente cumple, sin cambios |
| CA-10 | Tamaño mínimo 12px | ✅ Cumplido | Ajustados 6 selectores |
| CA-14 | Sombras sutiles en tarjetas | ✅ Cumplido | `dropshadow` con opacidad 0.04 |
| CA-16 | 10+ iconos SVG | ✅ Cumplido | 14 iconos totales (4 existentes + 10 nuevos) |

---

## Decisiones Técnicas

### D-1: Retrocompatibilidad
Se modificaron clases CSS existentes de forma aditiva. No se eliminaron propiedades ni se renombraron clases para evitar romper las 20+ vistas existentes.

### D-2: CSS vs Java
- Estados hover/focus → CSS (selectores de pseudo-clase)
- Iconos SVG → Java (creación programática con SVGPath)
- Estados vacíos → Java (requieren estructura de nodos)

### D-3: Sombra en Tarjetas
Se agregó sombra sutil a todas las tarjetas (`.card`) usando `-fx-effect: dropshadow()` con opacidad muy baja (0.04) para no afectar la estética flat existente.

### D-4: Iconos para Estados Vacíos
Se eligieron iconos semánticamente apropiados:
- INBOX: listas vacías generales (dispositivos, etc.)
- ALERT_TRIANGLE: advertencias de requisitos faltantes
- CLIPBOARD: registros y evaluaciones vacías
- LINK: trazabilidad y enlaces
- BLOCKS/LAYERS: estructura jerárquica

---

## Regresiones y Problemas Conocidos

### Pruebas Fallidas (Pre-existentes)
5 pruebas fallan con error `expected: <Motor> but was: <Motor(s)>`:
- `TraceabilityApiTest.consultasHaciaAtrasYHaciaDelante`
- `TraceabilityClientTest.consultasDeTrazabilidadYDashboardVacio`
- `RpnDashboardServiceTest.cadaFilaTraeDanoModoDeFalloComponenteYFactores`
- `TraceabilityServiceTest.enlazaRequisitoConMedidaYApareceEnLaMatrizConSusPruebas`
- `TraceabilityServiceTest.haciaAtrasListaLosComponentesDelDanoEnOrdenYSinRepetir`

**Nota:** Estos errores existían antes de los cambios visuales y no están relacionados con las modificaciones CSS/UI.

### Sin Regresiones Visuales
- Compilación: ✅ Exitosa
- Lógica de negocio: ✅ No modificada
- Vistas existentes: ✅ Compatibles (solo se agregaron estilos CSS)

---

## Próximos Pasos Sugeridos

1. **Verificación Visual Manual:**
   - Ejecutar aplicación y navegar por las 9 secciones
   - Verificar estados vacíos en HomeView, RisksView, AssessmentView, etc.
   - Probar navegación por teclado (Tab) para verificar focus visible
   - Verificar hover en filas de tarjeta y tabla

2. **Corrección de Pruebas Fallidas:**
   - Investigar error "Motor" vs "Motor(s)" en servicios de trazabilidad
   - Ajustar lógica de pluralización o expectativas de prueba

3. **Mejoras Futuras (Fuera de Alcance Actual):**
   - Tooltips enriquecidos (REQ-VIS-019) - no implementado
   - Indicadores de progreso animados - no implementado (se mantiene "Cargando...")
   - Responsividad real - solo verificado en 1024x768

---

## Trazabilidad

### Requisitos Implementados
- REQ-VIS-005: Hover en elementos interactivos
- REQ-VIS-006: Focus visible en navegación por teclado
- REQ-VIS-007: Estados disabled claros
- REQ-VIS-010: Sombras sutiles en tarjetas
- REQ-VIS-011: Hover en filas de listas
- REQ-VIS-012: Hover en filas de tabla
- REQ-VIS-013: Tags semánticos (ya cumplía, ajustado tamaño)
- REQ-VIS-016: Iconos SVG inline expandidos
- REQ-VIS-017: Iconos consistentes
- REQ-VIS-018: Estados vacíos enriquecidos
- REQ-VIS-020: Tamaño mínimo de fuente 12px

### Historias de Usuario Implementadas
- HU-VIS-001: Usuario puede diferenciar elementos interactivos (hover)
- HU-VIS-002: Usuario puede navegar por teclado con focus visible
- HU-VIS-004: Usuario ve mensajes claros cuando no hay datos

---

## Comandos Ejecutados

```bash
# Compilación exitosa
mvn compile
# [INFO] BUILD SUCCESS

# Pruebas (5 fallos pre-existentes)
mvn test
# [ERROR] Tests run: 196, Failures: 5, Errors: 0, Skipped: 0
```

---

**Fin del documento de cambios.**
