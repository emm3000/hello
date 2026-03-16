# Plan Operativo: Clean Architecture + Uncle Bob

Fecha: 2026-03-16

Objetivo: consolidar cumplimiento arquitectónico, reducir deuda técnica estructural y dejar enforcement sostenido en PR/CI.

## Principios guía

- Dependencias hacia adentro: `app -> domain <- data`.
- `domain` no depende de Android ni de infraestructura.
- Casos de uso en `domain` con nombre `*UseCase` y única API pública `invoke`.
- Reglas de negocio detrás de puertos/interfaces, no concretos.
- PR sin regresiones de calidad (`detekt` + tests verdes).

## Fase 1: Congelar reglas (1-2 días)

1. Formalizar reglas no negociables en documentación técnica.
2. Exigir en PR:
   - `./gradlew detekt`
   - tests del módulo afectado
3. Revisar checklist de arquitectura en cada PR como gate.

Entregables:
- checklist actualizado y usado en revisión.
- criterio de rechazo explícito para regresiones de arquitectura.

## Fase 2: Reducir deuda en `app` (1 sprint)

1. Priorizar issues de baseline por severidad:
   - `LongMethod`
   - `CyclomaticComplexMethod`
   - `TooGenericExceptionCaught`
   - `ImportOrdering` y naming baratos
2. Empezar por componentes de alta reutilización (`core/ui`) y pantallas grandes.
3. Medir progreso semanal.

Meta:
- bajar baseline de `app` de 72 a <= 35.

## Fase 3: Reducir deuda en `data` (1 sprint)

1. Priorizar:
   - `TooGenericExceptionCaught`
   - `LongMethod`
   - `ComplexCondition`
   - `ImportOrdering`
2. Mantener frontera: `data` implementa puertos de `domain`; no filtrar detalles a dominio.
3. Refactor por lotes pequeños con validación continua.

Meta:
- bajar baseline de `data` de 43 a <= 20.

## Fase 4: Endurecer enforcement (2-3 días)

1. Agregar chequeo automático de convención `*UseCase` en CI (script simple revisable).
2. Agregar verificación de imports prohibidos en `domain` (Android/Retrofit/SQLDelight).
3. Regla de equipo:
   - no se aceptan nuevos issues en baseline
   - baseline solo puede bajar

Entregables:
- tarea CI ejecutándose en PR.
- reporte de validación en cada pipeline.

## Fase 5: Legacy y consolidación (1 sprint)

1. Inventario del código legacy activo en runtime principal.
2. Definir por feature:
   - migrar
   - encapsular detrás de puerto
   - retirar
3. Ejecutar en orden de impacto funcional y riesgo.

Meta:
- reducir superficie legacy en el camino principal de la app.

## Criterios de cierre

1. `domain` baseline = 0 (mantener).
2. `app` baseline <= 10.
3. `data` baseline <= 10.
4. PRs siempre con `detekt` + tests verdes.
5. Sin imports prohibidos en `domain`.
6. Features activas bajo contrato consistente de presentación y casos de uso de dominio.

## Cadencia recomendada

1. Revisión semanal de métricas (`baseline` por módulo).
2. Objetivo de reducción por sprint.
3. Si una fase bloquea entregas funcionales, dividir en lotes más pequeños sin romper la regla de no regresión.
