# Arquitectura Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Estructura técnica del proyecto |
| Source of Truth | Yes |
| Read this when | Necesitás entender módulos, boundaries o wiring actual |

## Stack

- Kotlin
- Jetpack Compose
- Koin
- SQLDelight (`HelloDb`)
- Firebase AI con `gemini-2.5-flash-lite`

## Módulos

### `app`

- pantallas y navegación
- viewmodels MVI
- wiring de Koin
- startup

### `data`

- repositorios concretos
- persistencia local con SQLDelight
- identidad local de instalación
- generación de contenido con Firebase AI

### `domain`

- modelos y casos de uso
- sin Android
- sin DB
- sin network

## Dependencias

- `app -> data`
- `app -> domain`
- `data -> domain`

## Startup actual

Flujo vigente:

`App -> Koin -> AppStartupCoordinator.start() -> LocalIdentityInitializer.ensureReady()`

No hay otras etapas obligatorias de producto en startup.

## Persistencia actual

- `HelloDb` es source of truth
- decks, flashcards y reviews se leen desde estado local
- `DefaultFlashcardReviewRepository` persiste `ReviewEvent` y `ReviewProjection`

## Features relevantes hoy

- creación de tarjetas con preview editable y regeneraciones parciales
- estudio basado en múltiples `StudySessionItem` por flashcard
- consolidación de review una vez terminados los items de una flashcard

## Costura preservada

La única costura explícita para una posible vuelta del remoto es la identidad local:

- `LocalIdentityInitializer`
- `LocalIdentityState`
- `deviceId`

## Ver también

- `LOCAL_FIRST.md`
- `docs/CARD_CREATION_CURRENT.md`
- `docs/STUDY_CURRENT.md`
