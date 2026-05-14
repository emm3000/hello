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

## Navegación

- Entry composable: `NewRoot` en `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt`.
- Stack: Jetpack Navigation 3 (`NavDisplay`, `rememberNavBackStack`).
- Wrapper de backstack: clase `Navigator` (`app/.../navigation/Navigator.kt`).
- Transiciones: slide horizontal (350ms) para push/pop/predictive-pop.
- Routes activas: `DashboardRoute`, `StudyRoute(deckId)`, `NewCardRoute`, `NewDeckRoute(deckId)`, `DeckDetailRoute(deckId)`, `CardDetailRoute(cardId, deckId)`, `EditFlashcardRoute(cardId, deckId)`, `SettingsRoute`.
- Decorators: `rememberSaveableStateHolderNavEntryDecorator` + `rememberViewModelStoreNavEntryDecorator`.
- Startup gate: `NewRoot` observa `AppStartupViewModel` y muestra loading/error antes de `AppNavigation`.

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
