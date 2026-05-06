# Hello

| Field | Value |
|---|---|
| Status | Active |
| Role | Entrada principal del repo |
| Source of Truth | Parcial |
| Read this when | Entrás por primera vez al proyecto |

App Android en Kotlin + Jetpack Compose para estudiar con decks y flashcards.

## Estado actual

- producto local-first single-device
- `HelloDb` es source of truth de lectura y escritura
- startup solo asegura identidad local de instalación
- no hay runtime activo de sync remoto, pairing ni bootstrap remoto

## Módulos

- `app`: UI, navegación, DI y startup
- `data`: repositorios, SQLDelight, identidad local y Firebase AI
- `domain`: modelos y casos de uso JVM-only

## Leer en este orden

1. `AGENTS.md`
2. `ARCHITECTURE.md`
3. `LOCAL_FIRST.md`
4. `docs/README.md`

## Documentos activos

- `AGENTS.md`: reglas operativas del repo
- `ARCHITECTURE.md`: estructura técnica actual
- `LOCAL_FIRST.md`: contrato actual de runtime
- `docs/CARD_CREATION_CURRENT.md`: flujo actual de creación de tarjetas
- `docs/STUDY_CURRENT.md`: flujo actual de estudio

## No asumir

- multi-device
- sync remoto activo
- que referencias legacy de config impliquen comportamiento activo
