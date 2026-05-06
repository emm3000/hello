# Estudio Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Referencia factual de feature |
| Scope | Flujo `Study` |
| Source of Truth | No |
| Read this when | Necesitás entender cómo funciona hoy la sesión de estudio |

## Resumen

La sesión de estudio trabaja sobre una cola de `StudySessionItem` derivados de flashcards del deck.

Cada flashcard puede expandirse en múltiples items de estudio. La review se persiste una vez por flashcard, cuando se terminan sus items pendientes.

## Archivos clave

- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyUiEffect.kt`

## Estado actual

`StudyUiState` mantiene:

- `currentItem`
- `reviewedCount`
- `totalCount`
- `sessionFinished`

La lógica fuerte vive en `StudyScreen` y `StudyViewModel`.

## Carga de sesión

`StudyViewModel`:

- obtiene flashcards con `GetStudySessionUseCase(deckId)`
- expande cada flashcard a `StudySessionItem`
- guarda una cola local `ArrayDeque`
- inicializa `totalCount`
- muestra el primer item disponible

## Modelo de progreso

Por cada flashcard, el viewmodel mantiene:

- items pendientes por `flashcardId`
- grade agregado más conservador por `flashcardId`
- referencia a la flashcard original

Cuando el último item de una flashcard se responde:

- calcula el grade final más conservador
- agenda nueva review con `ScheduleFlashcardReviewUseCase`
- persiste con `UpdateFlashcardReviewUseCase`

## Etapas visuales

`StudyScreen` usa estas etapas locales:

- `Start`
- `Empty`
- `Recall`
- `Check`
- `Grade`

La etapa depende de:

- si la sesión empezó
- si hay item actual
- si la card necesita typed answer
- si la respuesta tipeada ya fue chequeada

## Flujo actual de interacción

### Start

Muestra una tarjeta de inicio con:

- cantidad total de items
- tiempo estimado
- CTA para empezar

### Recall

Muestra el frente de la card y CTA para revelar o pasar a responder.

### Check

Si la card requiere respuesta tipeada:

- muestra input
- permite comprobar respuesta
- permite revelar igual sin responder

### Grade

Muestra botones de grading según la política permitida para esa card y el resultado del typed answer.

## Typed answer

El estado local de pantalla mantiene:

- `typedAnswer`
- `typedAnswerChecked`
- `typedAnswerCorrect`

El matching se hace contra:

- `expectedAnswer`
- `acceptedAnswers`
- `evaluationMode`

## Navegación y cierre

Comportamientos actuales:

- si ya hubo progreso, back muestra confirmación de salida
- cuando termina la sesión, se emite `SessionFinished`
- al cerrar el diálogo final o back, se emite `NavigateBack`

## Efectos actuales

`StudyUiEffect` hoy expone:

- `NavigateBack`
- `SessionFinished`
