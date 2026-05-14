# Decks Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujos `Deck Detail` y `New/Edit Deck` |
| Source of Truth | No |
| Read this when | Necesitás entender la creación, edición y vista de detalle de mazos |

## Resumen

Dos flujos hermanos sobre la misma feature `deck`:

- `Deck Detail` muestra info del mazo, su lista de tarjetas con búsqueda local, y entry points a edición/eliminación.
- `New/Edit Deck` reutiliza la misma pantalla y viewmodel para crear o editar un mazo, distinguidos por `DeckFormMode`.

## Archivos clave

### Deck Detail

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailUiEffect.kt`

### New / Edit Deck

- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckFormMode.kt` (`Create` / `Edit(deckId)`)

## Deck Detail

### Estado

`DeckDetailUiState`:

- `deck: Deck` (default vacío con `SystemClock`)
- `hasSessionEnabled` (true si alguna tarjeta tiene `nextReviewAt <= now`)
- `searchQuery`
- `showDeleteConfirmation`

### Carga

`DeckDetailViewModel.init` combina dos flows:

- `GetDeckDetailUseCase(deckId)` — info del mazo + cards
- `ObserveFlashcardsWithReviewUseCase(deckId)` — flashcards con review schedule

El merge (`mergeDeckCardsById`) sobrescribe el `review` de las cards del deck con el del flow de study, manteniendo el resto de campos.

### Acciones

- `SearchCardsChanged(query)` — actualiza filtro local (matching por `word`, `translation`, `meaning`, case-insensitive)
- `EditDeck` → emite `NavigateToEditDeck(deckId)`
- `DeleteDeck` → abre confirmación
- `ConfirmDeleteDeck` → `SoftDeleteDeckUseCase` + emite `DeckDeleted`
- `DismissDeleteDeck` → cierra confirmación

### Efectos

`DeckDetailUiEffect`:

- `NavigateToEditDeck(deckId)`
- `DeckDeleted`
- `ShowMessage(text)`

## New / Edit Deck

### Estado

`NewDeckUiState`:

- `name`, `description`, `tags: List<String>` (normalizados: lowercase + trim + distinct + non-blank)
- `isLoading`
- `formMode: DeckFormMode` (`Create` o `Edit(deckId)`)
- `isValid` (computed): `name` no vacío

### Carga

Solo si `formMode is DeckFormMode.Edit`:

- `DeckRepository.findById(deckId).first()`
- popula `name`, `description`, `tags` desde el deck cargado

### Acciones

Intents:

- `NameChanged(name)`
- `DescriptionChanged(description)`
- `TagsChanged(tags)` — normaliza antes de guardar en state
- `Submit` — short-circuit si `!isValid || isLoading`

### Submit

- `DeckFormMode.Create` → `DeckRepository.addDeck(CreateDeckInput(...))` → reset state + `NavigateBack`
- `DeckFormMode.Edit` → `UpdateDeckUseCase(UpdateDeckInput(...))` → `NavigateBack`

### Efectos

`NewDeckUiEffect`:

- `NavigateBack`
- `ShowMessage(text)`

## Persistencia

- Lectura/escritura: 100% local sobre `HelloDb` vía repos y use cases del módulo `:domain`/`:data`.
- Soft delete preserva data y respeta `LOCAL_FIRST.md`.
