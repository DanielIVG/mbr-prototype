# Tesis: Prototipo de software de gestión de riesgos basado en modelos (MBR) para dispositivos médicos

## Contexto
- Maestría en UTP, Campus Chiriquí. El código es un prototipo académico.
- Marco normativo de referencia: ISO 14971 (gestión de riesgos de dispositivos médicos).

## Convenciones
- Documentación y comentarios en español; nombres de código en inglés.
- Todo requisito debe ser trazable a un elemento del modelo de riesgos.
- Pruebas obligatorias para la lógica de evaluación de riesgos.

## Comandos (Maven)
- Compilar: `mvn compile`
- Pruebas: `mvn test`
- Empaquetar: `mvn package`

## Estructura
- src/: código del prototipo
- docs/specs, docs/design, docs/normativo, docs/qa: salidas de los agentes
- docs-cambios/: bitácora de cambios de la tesis

## Flujo de equipo
Para toda funcionalidad nueva, delega en este orden pasando rutas de documentos, no resúmenes:
1. pm → docs/specs/
2. arquitecto → docs/design/
3. revisor-normativo → docs/normativo/ (si hay hallazgos CRÍTICOS, volver al arquitecto)
4. desarrollador → implementación y pruebas
5. qa → docs/qa/ (si falla, volver al desarrollador; máximo 3 iteraciones, luego consultarme)
6. documentador-tesis → docs-cambios/
