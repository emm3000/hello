# Edición de Tarjeta Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `Edit Flashcard` |
| Source of Truth | No |
| Read this when | Necesitás entender cómo se editan los campos de una tarjeta existente |

## Resumen

`Edit Flashcard` carga una tarjeta existente, permite editar sus campos básicos y ejemplos, valida en vivo y persiste el cambio vía `UpdateFlashcardUseCase`. Se abre desde `Card Detail`.

## Archivos clave

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/EditFlashcardUiEffect.kt`

## Estado

`EditFlashcardUiState`:

- `isLoading` (true mientras carga la tarjeta)
- campos editables: `word`, `meaning`, `translation`, `phonetic`, `partOfSpeech`, `examples: List<Example>`
- errores: `wordError`, `meaningError`
- `isSubmitting`
- `isValid` (computed): `word` y `meaning` no vacíos y sin errores

## Carga

`EditFlashcardViewModel.init` llama `loadFlashcard()`:

- `FlashcardRepository.fetchById(flashcardId)`
- en éxito popula los campos desde `detail.flashcard`
- en error: `ShowMessage` + `isLoading = false`

## Acciones

Intents soportadas:

- `WordChanged(text)` — valida no-blank
- `MeaningChanged(text)` — valida no-blank
- `TranslationChanged(text)`
- `PhoneticChanged(text)`
- `PartOfSpeechChanged(text)`
- `ExampleTextChanged(index, text)`
- `ExampleTranslationChanged(index, translation)`
- `AddExample` — agrega `Example` vacío al final
- `RemoveExample(index)` — bounded por `examples.indices`
- `Submit` — short-circuit si `!isValid || isSubmitting`

## Submit

`handleSubmit()`:

- valida estado
- arma `UpdateFlashcardInput(flashcardId, deckId, word, meaning, translation, phonetic, partOfSpeech, examples)`
- llama `UpdateFlashcardUseCase`
- en éxito: emite `NavigateBack`
- en error: `ShowMessage` + libera `isSubmitting`

## Efectos

`EditFlashcardUiEffect`:

- `NavigateBack`
- `ShowMessage(text)`

## Persistencia

- Lectura: `FlashcardRepository.fetchById` (local).
- Escritura: `UpdateFlashcardUseCase` (local).
- No hay sync remoto involucrado.
