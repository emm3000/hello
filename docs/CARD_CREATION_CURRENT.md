# Creación de Tarjetas Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `New Card` |
| Source of Truth | No |
| Read this when | Necesitás entender cómo funciona hoy la creación de tarjetas |

## Resumen

El flujo actual de creación tiene 3 pasos manejados en `NewCardRoute`:

1. `Mode`
2. `Input`
3. `Review`

La navegación es local al route y usa un único `NewCardViewModel` compartido.

## Archivos clave

Estos son **entry points y artefactos principales** del flujo de creación. El resto de `newfeatures/card/` contiene componentes internos (preview UI, validación, drafts) y los flujos hermanos de detalle/edición documentados aparte.

- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardModeScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardInputStepScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardReviewScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardGenerationMappings.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardPreviewWorkflow.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardDraftEditor.kt`

## Flujos relacionados

- Ver tarjeta existente: `docs/CARD_DETAIL_CURRENT.md`
- Editar tarjeta existente: `docs/EDIT_FLASHCARD_CURRENT.md`

## Paso 1. Modo

`NewCardModeScreen` muestra un selector de modo y un CTA para continuar.

Modos actuales de `TypeView`:

- `WordOrPhase`
- `WithCategories`
- `WithAiHelp`

## Paso 2. Input

`NewCardInputStepScreen` muestra:

- inputs según `TypeView`
- selección de deck
- checkbox para deck por defecto
- CTA `Generar`

Habilitación actual:

- `WordOrPhase`: requiere deck y `word`
- `WithCategories`: requiere deck
- `WithAiHelp`: requiere deck y `aiRequest`

Soporte actual:

- micrófono en inputs de palabra
- categorías estáticas mediante bottom sheet
- dificultad simple mapeada a `LevelBand`

## Input de dominio

`NewCardUiState` se traduce a `FlashcardGenerationInput` en `NewCardGenerationMappings.kt`.

Mapeo actual:

- `WordOrPhase` infiere `Word`, `Phrase` o `Sentence` desde `word`
- `WithCategories` usa `CommunicativeGoal` desde una categoría estática
- `WithAiHelp` usa `CommunicativeGoal` desde texto libre

Antes de generar preview siempre hay validación de input.

## Paso 3. Review

`NewCardReviewScreen` renderiza uno de estos estados:

- preview disponible
- loading
- error
- empty state

La review actual permite:

- editar campos de `GeneratedLearningNote`
- editar prompt, expected answer e hint de cada `GeneratedStudyCard`
- activar o desactivar cards individuales
- regenerar ejemplo
- regenerar cloze
- regenerar campos específicos (`WhyUseful`, `UsagePattern`, `CommonMistake`)
- regenerar una card individual

La validación se recalcula después de cada edición o regeneración.

## Guardado

`NewCardViewModel.saveFlashcard()`:

- exige `deckSelected`
- exige `learningNotePreview`
- vuelve a validar preview antes de guardar
- usa `CreateFlashcardUseCase`
- al guardar con éxito resetea estado, muestra mensaje y cierra el flujo

## Efectos del flujo

`NewCardUiEffect` hoy expone:

- `ShowMessage`
- `OpenReview`
- `CloseFlow`

`GenerateClicked` dispara `OpenReview` antes de resolver el resultado, así que el paso de review también contiene loading y errores.

## Modelo relevante

La preview gira alrededor de `GeneratedLearningNote`:

- nota base
- ejemplo
- metadata lingüística
- `cards`
- `qualityChecks`
- `warnings`

La detección de duplicados exactos existe mediante `FlashcardDuplicateRepository`.
