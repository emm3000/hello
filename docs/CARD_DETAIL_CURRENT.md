# Detalle de Tarjeta Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `Card Detail` |
| Source of Truth | No |
| Read this when | Necesitás entender cómo se muestra y se elimina una tarjeta existente |

## Resumen

`Card Detail` muestra una flashcard guardada y permite ir a edición o eliminarla (soft delete). Se abre desde el deck detail o desde la dashboard.

## Archivos clave

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/CardDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailUiEffect.kt`

## Estado

`FlashcardDetailUiState` mantiene:

- `flashcard: FlashcardDetail` (default vacío con `SystemClock`)
- `showDeleteConfirmation`

## Carga

`FlashcardDetailViewModel.init`:

- dispara `FlashcardDetailUiIntent.Load`
- pide `FlashcardRepository.fetchById(flashcardId)`
- en error emite `LoadFailed(message)`

## Acciones

- `EditFlashcard` → emite `NavigateToEditFlashcard(flashcardId)`
- `DeleteFlashcard` → abre diálogo de confirmación (`showDeleteConfirmation = true`)
- `ConfirmDeleteFlashcard` → usa `SoftDeleteFlashcardUseCase` y emite `FlashcardDeleted`
- `DismissDeleteFlashcard` → cierra diálogo

## Efectos

`FlashcardDetailUiEffect`:

- `LoadFailed(message)`
- `NavigateToEditFlashcard(flashcardId)`
- `FlashcardDeleted`
- `ShowMessage(text)`

## Persistencia

- Lectura: `FlashcardRepository.fetchById` (local).
- Delete: soft delete vía `SoftDeleteFlashcardUseCase`.
- No hay sync remoto involucrado.
