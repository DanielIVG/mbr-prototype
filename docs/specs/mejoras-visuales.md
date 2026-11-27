# Especificación: Mejoras visuales de la interfaz

**Producto:** MBR - Prototipo de gestión de riesgos basado en modelos para dispositivos médicos
**Versión:** 1.0
**Fecha:** 2026-11-27
**Responsable:** Product Manager

---

## 1. Objetivo y alcance

### 1.1 Objetivo
Mejorar la experiencia visual del usuario en el prototipo MBR mediante refinamientos de diseño que aumenten la claridad, consistencia y usabilidad de todas las pantallas, sin alterar la funcionalidad existente ni la arquitectura de datos.

### 1.2 Alcance
**Incluye:**
- Refinamiento de estilos CSS existentes en `src/main/resources/css/mbr.css`
- Mejoras de consistencia visual entre las 20+ vistas de la aplicación
- Optimización de espaciado, tipografía y jerarquía visual
- Enriquecimiento de estados interactivos (hover, focus, loading, error, vacío)
- Iconografía complementaria y elementos gráficos de apoyo
- Mejoras de legibilidad y contraste para accesibilidad básica
- Feedback visual claro para acciones del usuario
- Estados de carga y transiciones suaves donde sea pertinente

**NO incluye:**
- Cambios en la funcionalidad o lógica de negocio
- Modificación del modelo de datos o endpoints de API
- Rediseño completo de la paleta de colores (se mantiene #15616D y #F5F4F1)
- Implementación de temas alternativos (modo oscuro, etc.)
- Animaciones complejas o efectos que afecten el rendimiento
- Cambios en la estructura de navegación o flujos de trabajo
- Implementación de nueva funcionalidad
- Generación de documentación de usuario

---

## 2. Historias de usuario

### HU-VIS-001: Navegación visual clara
**Como** usuario del sistema MBR
**Quiero** reconocer inmediatamente en qué sección estoy y cuáles acciones están disponibles
**Para** orientarme rápidamente y trabajar con mayor confianza

**Notas:**
- Aplica a las 9 secciones del workspace: Estructura, Comportamiento, Riesgos, Criterios, Evaluación, Modelo, Análisis (FTA), Dashboard RPN y Trazabilidad
- El estado activo del panel de navegación debe ser inequívoco
- Las migas de pan (breadcrumbs) deben reflejar siempre el contexto actual

### HU-VIS-002: Feedback interactivo consistente
**Como** usuario interactuando con formularios y controles
**Quiero** recibir retroalimentación visual inmediata cuando interactúo con elementos
**Para** saber que el sistema responde y entender qué elementos son accionables

**Notas:**
- Incluye botones, enlaces, campos de texto, tarjetas seleccionables, filas de listas
- Estados: normal, hover, focus, disabled, error, loading
- Debe aplicarse de forma consistente en las 20+ vistas

### HU-VIS-003: Jerarquía visual de información
**Como** usuario revisando pantallas con múltiples niveles de información
**Quiero** distinguir claramente títulos, datos principales y detalles secundarios
**Para** escanear la información rápidamente y encontrar lo que necesito

**Notas:**
- Aplica especialmente a: RisksView (lista de cadenas), AssessmentView (formularios complejos), TraceabilityView (matriz), RpnDashboardView (KPIs y tabla)
- Uso efectivo de peso tipográfico, tamaño y color

### HU-VIS-004: Estados vacíos informativos
**Como** usuario nuevo o trabajando en un dispositivo recién creado
**Quiero** que las pantallas vacías me expliquen qué hacer y por qué están vacías
**Para** no sentirme perdido y saber cómo proceder

**Notas:**
- Aplica a: lista de dispositivos vacía, componentes sin definir, cadenas de riesgo inexistentes, evaluaciones pendientes
- Los estados vacíos actuales son funcionales pero pueden enriquecerse visualmente

### HU-VIS-005: Indicadores de estado claros
**Como** usuario gestionando riesgos y evaluaciones
**Quiero** reconocer de inmediato el estado de cada elemento (aceptable/no aceptable, con/sin medida de control, etc.)
**Para** priorizar mi trabajo y tomar decisiones informadas

**Notas:**
- Aplica a: chips/tags de estado (aceptable, no aceptable, sin estimar)
- Uso consistente de color semántico: verde=ok, rojo=problema, amarillo=advertencia, gris=neutral

### HU-VIS-006: Formularios legibles y manejables
**Como** usuario completando formularios extensos (estimación, FTA, comportamiento)
**Quiero** que los campos estén bien organizados y los errores sean evidentes
**Para** completar la información correctamente sin frustración

**Notas:**
- Formularios críticos: AssessmentView (estimación con cantidades), FtaView (árbol), BehaviorView (nodos), TraceabilityView (requisitos)
- Validación en línea, mensajes de error claros

### HU-VIS-007: Tablas y listas escaneables
**Como** usuario revisando tablas extensas (dashboard RPN, trazabilidad, lista de dispositivos)
**Quiero** que las filas sean fáciles de distinguir y la información esté bien alineada
**Para** procesar rápidamente grandes volúmenes de datos

**Notas:**
- Aplica a: HomeView (lista de dispositivos), RpnDashboardView (tabla RPN), TraceabilityView (matriz de trazabilidad)
- Hover en filas, alineación de columnas, encabezados claros

### HU-VIS-008: Accesibilidad básica
**Como** usuario con necesidades de accesibilidad (contraste, tamaño de fuente)
**Quiero** que el texto sea legible y los colores tengan suficiente contraste
**Para** usar el sistema sin fatiga visual

**Notas:**
- Cumplimiento mínimo de contraste WCAG AA (4.5:1 para texto normal, 3:1 para texto grande)
- Tamaños de fuente no menores a 12px (actualmente el mínimo es 11px en table-head)

---

## 3. Requisitos funcionales y no funcionales

### 3.1 Consistencia visual

**REQ-VIS-001: Paleta de colores consistente**
El sistema DEBE mantener la paleta de colores actual (#15616D teal, #F5F4F1 fondo, #1C1917 texto) y aplicarla consistentemente en todos los componentes.

**REQ-VIS-002: Tipografía uniforme**
El sistema DEBE usar la jerarquía tipográfica definida en `mbr.css` de forma consistente: page-title (28px), form-title (24px), form-title-sm (17px), section-title (14px), field-label (13px), hint (12px), table-head (11px).

**REQ-VIS-003: Espaciado coherente**
El sistema DEBE aplicar múltiplos de 4px para padding, margin y gaps (4, 8, 12, 16, 20, 24, 28, etc.) en todos los componentes.

**REQ-VIS-004: Bordes y radios uniformes**
El sistema DEBE usar border-radius de 7px para campos/botones, 8px para tarjetas pequeñas, 10px para tarjetas principales, consistentemente en todos los componentes.

### 3.2 Estados interactivos

**REQ-VIS-005: Hover en elementos accionables**
Todos los elementos accionables (botones, enlaces, filas seleccionables, tiles) DEBEN tener un estado hover visualmente distinto con cambio de color o borde.

**REQ-VIS-006: Focus visible**
Todos los controles interactivos DEBEN mostrar un indicador de focus visible (borde teal #15616D de 2px) cuando se navega con teclado.

**REQ-VIS-007: Estados disabled claros**
Los elementos deshabilitados DEBEN tener opacidad reducida (0.5-0.6) y cursor default, sin cambio en hover.

**REQ-VIS-008: Feedback de carga**
Las operaciones asíncronas DEBEN mostrar un indicador de "Cargando..." o similar mientras se espera la respuesta del servidor.

**REQ-VIS-009: Mensajes de error destacados**
Los mensajes de error DEBEN usar la clase `field-error` (#9B2C2C) y ubicarse cerca del control afectado, con suficiente contraste.

### 3.3 Componentes específicos

**REQ-VIS-010: Tarjetas con sombra sutil**
Las tarjetas (clase `.card`) PUEDEN incluir una sombra sutil (box-shadow) para mejorar la percepción de profundidad sin sobrecargar visualmente.

**REQ-VIS-011: Listas con separadores claros**
Las listas verticales (`.list-row`) DEBEN tener separación visual clara entre elementos, ya sea con bordes, espaciado o fondo alterno.

**REQ-VIS-012: Tablas con hover en filas**
Las tablas (TableView de JavaFX) DEBEN aplicar un fondo de hover (#F1F7F8 o similar) en filas para facilitar el seguimiento visual.

**REQ-VIS-013: Chips/tags semánticos**
Los chips de estado (`.tag`, `.tag-ok`, `.tag-bad`, `.tag-warn`) DEBEN usar colores semánticos consistentes:
- Verde (#DDF0E4 / #0E6B3A) para estados positivos/aceptables
- Rojo (#F8DCDC / #7A1F1F) para estados negativos/no aceptables
- Amarillo (#FBEFD3 / #7A5300) para advertencias
- Teal (#DCEBED / #0E434B) para estados neutrales/informativos

**REQ-VIS-014: Botones primarios destacados**
Los botones primarios (`.btn-primary`) DEBEN destacarse claramente del fondo y botones secundarios mediante su color de relleno teal (#15616D).

**REQ-VIS-015: Panel de navegación con estado activo claro**
El panel de navegación del workspace DEBE mostrar el ítem activo con fondo teal claro (#E6F0F1) y texto oscuro (#0E434B, weight 600).

### 3.4 Iconografía

**REQ-VIS-016: Iconos SVG inline**
Los iconos DEBEN implementarse como SVGPath inline (ya existente en `Ui.java`) con stroke consistente de 2px y line-cap/join round.

**REQ-VIS-017: Iconos en botones de acción**
Los botones de acción principales (crear, agregar, eliminar) PUEDEN incluir iconos para reforzar su significado.

**REQ-VIS-018: Iconos en estados vacíos**
Los estados vacíos (empty states) PUEDEN incluir un icono grande (`.empty-icon-box` 64x64px) para mejorar la comunicación visual.

### 3.5 Accesibilidad

**REQ-VIS-019: Contraste de texto mínimo**
El texto normal DEBE tener un ratio de contraste de al menos 4.5:1 con su fondo; el texto grande (18px+) al menos 3:1 (WCAG AA).

**REQ-VIS-020: Tamaño mínimo de fuente**
El tamaño de fuente mínimo DEBE ser 12px. La clase `.table-head` (actualmente 11px) DEBE ajustarse a 12px.

**REQ-VIS-021: Tamaño mínimo de targets táctiles**
Los botones y controles interactivos DEBEN tener un área mínima de 32x32px para facilitar la interacción (ya se cumple en botones principales).

### 3.6 Responsividad

**REQ-VIS-022: Adaptación a ventanas pequeñas**
El layout DEBE mantener funcionalidad en ventanas de al menos 1024x768px sin scroll horizontal excesivo.

**REQ-VIS-023: Texto wrappable**
Los textos largos (descripciones, finalidad prevista, etc.) DEBEN tener `setWrapText(true)` y ancho máximo definido para evitar desbordamiento.

### 3.7 Rendimiento visual

**REQ-VIS-024: Scroll suave**
Los ScrollPane DEBEN mantener el estilo minimalista actual (`.plain-scroll`) con scrollbars sutiles que no distraigan.

**REQ-VIS-025: Sin animaciones pesadas**
El sistema NO DEBE incluir animaciones complejas que afecten el rendimiento en máquinas modestas.

---

## 4. Criterios de aceptación

### CA-1: Consistencia de estilos CSS
**Verificación:** Revisión manual de `mbr.css`.
**Criterio:** Todas las clases CSS usan valores de espaciado múltiplos de 4px, border-radius consistente (7px campos, 8px tarjetas pequeñas, 10px tarjetas grandes), y colores de la paleta definida. No hay valores hardcodeados inconsistentes.

### CA-2: Estados hover funcionales
**Verificación:** Interacción manual con cada tipo de componente (botones, enlaces, tiles, filas de lista).
**Criterio:** Todos los elementos accionables cambian visualmente al hacer hover (cambio de color de fondo, borde o texto). Los elementos disabled no responden a hover.

### CA-3: Focus visible en navegación por teclado
**Verificación:** Navegación con Tab por las 9 secciones del workspace y formularios principales.
**Criterio:** Todos los controles muestran un borde teal de 2px al recibir focus. El orden de tabulación es lógico.

### CA-4: Mensajes de error visibles
**Verificación:** Provocar errores en formularios (campo vacío, dato inválido, error de servidor).
**Criterio:** El mensaje de error aparece en color rojo (#9B2C2C), cerca del control afectado, con texto legible y wrap activado.

### CA-5: Estados vacíos informativos
**Verificación:** Crear dispositivo nuevo y revisar cada sección vacía.
**Criterio:** Cada estado vacío muestra un mensaje claro explicando qué falta y cómo proceder. Opcionalmente incluye icono representativo.

### CA-6: Chips de estado semánticos
**Verificación:** Revisión visual en AssessmentView (aceptable/no aceptable), TraceabilityView (con/sin medida), RpnDashboardView.
**Criterio:** Los chips usan colores semánticos consistentes: verde para OK, rojo para problemas, amarillo para advertencias, teal para neutral.

### CA-7: Jerarquía visual en formularios complejos
**Verificación:** Abrir AssessmentView (formulario de estimación), FtaView (árbol), BehaviorView (nodos).
**Criterio:** Los títulos de sección, labels de campo y hints tienen tamaños y pesos diferenciados. La información secundaria (hints) usa color gris (#6B6660 o #8A857E).

### CA-8: Hover en filas de tabla
**Verificación:** Interactuar con RpnDashboardView (tabla), TraceabilityView (matriz), HomeView (lista de dispositivos).
**Criterio:** Al pasar el mouse sobre una fila, esta cambia de fondo (#F1F7F8 o similar) para facilitar el seguimiento visual.

### CA-9: Contraste WCAG AA
**Verificación:** Usar herramienta de contraste (ej. WebAIM Contrast Checker) en combinaciones texto/fondo principales.
**Criterio:** Texto normal (#1C1917) sobre fondo claro (#F5F4F1 o #FFFFFF) tiene ratio ≥ 4.5:1. Texto de hints (#6B6660) tiene ratio ≥ 4.5:1. Texto blanco sobre teal (#15616D) tiene ratio ≥ 4.5:1.

### CA-10: Tamaño de fuente mínimo 12px
**Verificación:** Revisión de `mbr.css` y renderizado en pantalla.
**Criterio:** Ninguna clase CSS define font-size menor a 12px. La clase `.table-head` se ajusta de 11px a 12px.

### CA-11: Indicadores de carga presentes
**Verificación:** Provocar carga de datos en secciones principales (Riesgos, Evaluación, Trazabilidad, Dashboard RPN).
**Criterio:** Mientras se carga, se muestra un mensaje "Cargando..." o similar. No hay pantalla en blanco sin feedback.

### CA-12: Navegación del workspace con estado activo claro
**Verificación:** Navegar por las 9 secciones del workspace.
**Criterio:** El ítem activo en el panel de navegación tiene fondo teal claro (#E6F0F1), texto oscuro (#0E434B) y peso 600. Los demás ítems tienen fondo blanco.

### CA-13: Botones primarios destacados
**Verificación:** Revisión visual de pantallas con botones primarios (crear dispositivo, estimar riesgo, guardar requisito, etc.).
**Criterio:** Los botones primarios usan fondo teal (#15616D) y texto blanco, destacándose claramente de los secundarios (fondo blanco, borde gris).

### CA-14: Tarjetas con profundidad visual
**Verificación:** Revisión de tarjetas en HomeView (tiles), WorkspaceView (resumen), RisksView (camino de falla).
**Criterio:** Las tarjetas tienen borde sutil (#E0DED8) y opcionalmente sombra ligera. El fondo blanco (#FFFFFF) contrasta con el fondo de página (#F5F4F1).

### CA-15: Listas con separación clara
**Verificación:** Revisión de RisksView (lista de cadenas), BehaviorView (árbol de nodos), TraceabilityView (lista de requisitos).
**Criterio:** Las filas de lista tienen separación visual mediante borde inferior (#EFEDE8) o espaciado vertical (8px). No hay ambigüedad en dónde termina un elemento y empieza otro.

### CA-16: Iconos consistentes
**Verificación:** Revisión de iconos en HomeView (tiles), estados vacíos.
**Criterio:** Todos los iconos SVG tienen stroke de 2px, line-cap round, line-join round. Los colores son teal (#15616D) o blanco según contexto.

### CA-17: Scroll sin distracciones
**Verificación:** Scroll en listas largas (Riesgos, Evaluación, Trazabilidad).
**Criterio:** Las scrollbars son sutiles (thumb #CFCBC3), sin flechas, y no ocupan espacio innecesario (9px ancho/alto). El scroll es funcional y no distrae.

### CA-18: Adaptación a 1024x768
**Verificación:** Redimensionar ventana a 1024x768px y navegar por las pantallas principales.
**Criterio:** Todas las pantallas son funcionales sin scroll horizontal excesivo. El texto largo hace wrap. Los paneles laterales (navegación, detalles) se ajustan o colapsan si es necesario.

### CA-19: Texto wrappable en columnas
**Verificación:** Revisar tablas con texto largo (TraceabilityView, RpnDashboardView, HomeView).
**Criterio:** Los labels y descripciones largas tienen `setWrapText(true)` y `maxWidth` definido para evitar que se corten con "...".

### CA-20: Sin animaciones pesadas
**Verificación:** Interacción general con la aplicación, cambio de secciones, apertura de formularios.
**Criterio:** No hay animaciones de entrada/salida complejas. Los cambios de estado son instantáneos o con transiciones CSS sutiles (< 200ms).

---

## 5. Priorización

### Crítico (bloqueante para usabilidad básica)
- REQ-VIS-005: Hover en elementos accionables
- REQ-VIS-006: Focus visible
- REQ-VIS-009: Mensajes de error destacados
- REQ-VIS-019: Contraste de texto mínimo
- REQ-VIS-020: Tamaño mínimo de fuente
- CA-2, CA-3, CA-4, CA-9, CA-10

### Importante (mejora significativa de UX)
- REQ-VIS-001 a REQ-VIS-004: Consistencia visual
- REQ-VIS-007, REQ-VIS-008: Estados disabled y carga
- REQ-VIS-011, REQ-VIS-012: Listas y tablas
- REQ-VIS-013, REQ-VIS-014, REQ-VIS-015: Componentes semánticos
- REQ-VIS-023: Texto wrappable
- CA-1, CA-5, CA-6, CA-7, CA-11, CA-12, CA-13, CA-15, CA-19

### Deseable (pulido y refinamiento)
- REQ-VIS-010: Sombras en tarjetas
- REQ-VIS-016 a REQ-VIS-018: Iconografía
- REQ-VIS-021: Targets táctiles
- REQ-VIS-022: Adaptación a ventanas pequeñas
- REQ-VIS-024, REQ-VIS-025: Scroll y rendimiento
- CA-8, CA-14, CA-16, CA-17, CA-18, CA-20

---

## 6. Preguntas abiertas

1. **Iconografía expandida:** ¿Se requiere una biblioteca de iconos más amplia (ej. más allá de los 4 actuales: plus, grid, doc, blocks) para enriquecer los estados vacíos y botones? ¿Debe integrarse una librería externa (Feather, Lucide) o mantenerse inline SVG?

2. **Sombras en tarjetas:** ¿Se prefiere un estilo flat (solo bordes) o con sombras sutiles (box-shadow) para las tarjetas? La tendencia del diseño actual es flat; ¿se mantiene o se introduce profundidad?

3. **Tablas nativas de JavaFX:** Las TableView de JavaFX tienen estilos limitados por CSS. ¿Se acepta una personalización parcial o se requiere reemplazo con componentes custom (más trabajo de desarrollo)?

4. **Tooltips enriquecidos:** Actualmente se usan Tooltip nativos de JavaFX. ¿Se requieren tooltips más ricos (con títulos, ejemplos, links) o es suficiente el texto simple?

5. **Indicadores de progreso:** Más allá del mensaje "Cargando...", ¿se requieren spinners animados o progress bars para operaciones largas? ¿Qué librerías gráficas están permitidas (ControlsFX, etc.)?

6. **Responsividad real:** ¿Se requiere soporte para ventanas menores a 1024x768 (ej. laptops de 1366x768 en modo ventana)? ¿Se permite colapsar paneles laterales o implementar layouts adaptativos?

7. **Modo oscuro (futuro):** ¿Se prevé implementar un tema oscuro en versiones futuras? Si es así, ¿debe estructurarse el CSS con variables CSS (no soportadas nativamente en JavaFX 17) o mediante clases alternativas?

8. **Densidad de información:** ¿Se prefiere un diseño más espaciado (actual) o compacto para maximizar información visible? Esto afecta padding, font-size y row-height de tablas.

9. **Accesibilidad avanzada:** Más allá de contraste y tamaño de fuente, ¿se requiere soporte para lectores de pantalla (ARIA labels en JavaFX) o navegación completa por teclado con shortcuts?

10. **Prototipo vs. producto final:** ¿Qué nivel de pulido visual se espera para el prototipo académico? ¿Se prioriza la velocidad de implementación o la calidad de acabado profesional?

---

## 7. Anexos

### 7.1 Paleta de colores actual (referencia)
- **Primario teal:** #15616D (acento, botones primarios, focus)
- **Teal hover:** #0E434B (hover de botones primarios)
- **Teal claro:** #E6F0F1 (navegación activa), #DCEBED (tags neutral), #CDE3E6 (borders sutiles)
- **Fondo página:** #F5F4F1
- **Fondo tarjeta:** #FFFFFF
- **Fondo tarjeta header:** #FAF9F7
- **Texto principal:** #1C1917
- **Texto secundario:** #6B6660, #8A857E (hints), #A8A39B (deshabilitado)
- **Bordes:** #E0DED8 (tarjetas), #CFCBC3 (campos), #E3E0DA (listas), #EFEDE8 (separadores sutiles)
- **Estados:**
  - Verde: #DDF0E4 fondo / #0E6B3A texto (OK)
  - Rojo: #F8DCDC fondo / #7A1F1F texto (error), #FBEAEA / #9B2C2C (error fuerte)
  - Amarillo: #FBEFD3 fondo / #7A5300 texto (warn), #FBF0DC / #7A5410 (banner)

### 7.2 Tipografía actual (referencia)
- **Familia:** "Inter", "Segoe UI", "SF Pro Text", "Helvetica Neue", Arial, sans-serif
- **Tamaños:**
  - 28px (page-title, weight 600)
  - 24px (form-title, weight 600)
  - 20px (verdict-ok/bad, weight 700)
  - 19px (empty-title, weight 600)
  - 17px (form-title-sm, weight 600)
  - 15px (tile-title, weight 600; crumb-current-strong, weight 600)
  - 14px (base, row-title weight 500/600, field-label weight 600)
  - 13px (app-subtitle, crumb, field-label, btn-link, tile-text, path-text)
  - 12px (version-label, hint, field-error, chip, pill-soon)
  - 11px (table-head, weight 600) **← ajustar a 12px**

### 7.3 Vistas afectadas (referencia)
1. **HomeView.java** - Pantalla de inicio, lista de dispositivos
2. **WorkspaceView.java** - Área de trabajo, navegación entre secciones
3. **StructureView.java** - Gestión de componentes
4. **BehaviorView.java** - Árboles de comportamiento
5. **RisksView.java** - Lista de cadenas de riesgo
6. **RiskTreeView.java** - Visualización de árbol de riesgos
7. **CriteriaView.java** - Configuración de criterios de aceptabilidad
8. **AssessmentView.java** - Formularios de estimación y evaluación
9. **FtaView.java** - Análisis de árbol de fallas
10. **ModelGraphView.java** - Modelo de riesgos
11. **RpnDashboardView.java** - Dashboard de RPN
12. **TraceabilityView.java** - Matriz de trazabilidad
13. **DeviceFormView.java** - Formulario de dispositivo
14. **NewChainView.java** - Creación de cadena de riesgo
15. **QuantityForm.java** - Formulario de cantidades (reutilizable)
16. **ChainPath.java** - Visualización de camino de falla
17. **Ui.java** - Componentes reutilizables

---

**Fin del documento de especificación.**
