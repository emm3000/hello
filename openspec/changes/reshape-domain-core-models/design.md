# Design: Reshape domain core models

Esta fase deja de mover packages por ownership y pasa a rehacer el modelo del dominio. La idea es cortar el acoplamiento semántico en tres puntos: nota generada, agregado persistido y sesión de estudio.

## Quick path

1. `GeneratedLearningNote` pasa a `generation` con sus tipos afines.
2. `study` obtiene un contrato propio de sesión.
3. `Flashcard` se adelgaza hasta quedar como agregado persistido/authoring.

## Architecture Decisions

| Topic | Choice | Alternatives considered | Rationale |
|-------|--------|-------------------------|-----------|
| Generated note ownership | Mover `GeneratedLearningNote` a `generation` | Dejarlo en `flashcard` por compatibilidad | Hoy es un modelo generativo puro; el package actual miente |
| Study contract | Crear `StudyFlashcard` o `StudySessionCard` específico | Seguir usando `Flashcard` completo | `study` no necesita metadata de authoring ni artifacts de generación |
| Flashcard slimming | Mantener `Flashcard` persistido y extraer submodelos/boundaries | Seguir agregando campos al mismo data class | El agregado actual mezcla demasiadas razones de cambio |
| Mappers | Centralizar adapters en boundaries explícitos | Multiplicar extensiones sueltas en varios archivos | El repo ya sufre semántica cruzada; más extensiones la empeoran |

## Data Flow

```text
generation.GeneratedLearningNote
  -> authoring mapper/boundary
  -> flashcard persisted aggregate

study repository
  -> study session model
  -> app StudySessionItem

flashcard review repository
  -> FlashcardReview
  -> study scheduler
```

## Target model shape

| Area | Owns |
|------|------|
| `generation` | `GeneratedLearningNote`, generated cards/checks, generation validation |
| `authoring` | mapping note -> persisted input, duplicate checks, create flow |
| `flashcard` | persisted flashcard aggregate and repositories |
| `study` | session contract/model, scheduling use cases, review-facing data |

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/.../flashcard/GeneratedLearningNote.kt` | Move/Modify | Reubicar a `generation` y ajustar imports dependientes |
| `domain/.../flashcard/GeneratedStudyCard.kt` | Evaluate/Move | Mover si queda estrictamente generativo o duplicarlo como study model si no |
| `domain/.../flashcard/GeneratedNoteQualityCheck.kt` | Move | Reubicar a `generation` |
| `domain/.../flashcard/Flashcard.kt` | Modify | Extraer o reducir campos para dejar foco persistido |
| `domain/.../study/*` | Create/Modify | Introducir modelo de sesión específico |
| `data/.../flashcard/DefaultFlashcardRepository.kt` | Modify | Mapear DB a aggregate persistido y a contrato study por separado |
| `app/.../study/StudySessionItem.kt` | Modify | Consumir modelo de study en vez de `Flashcard` completo |

## Interfaces / Contracts

```kotlin
data class StudyFlashcard(
    val flashcardId: FlashcardId,
    val review: FlashcardReview,
    val prompts: List<StudyPrompt>
)

interface StudySessionRepository {
    suspend fun sessionToday(deckId: DeckId): List<StudyFlashcard>
}
```

El nombre exacto puede variar, pero el contrato debe ser de `study`, no de `flashcard`.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit | `GeneratedLearningNote` y su move a `generation` | Mover tests y preservar guards semánticos |
| Unit | Nuevo modelo/mapper de `study` | Tests específicos para session contract |
| Integration | Adaptación de repositorios `data` | Validar mapping DB -> flashcard y DB -> study model |
| App | `StudyViewModel` y create flow | Ajustar tests al nuevo contrato sin cambiar UX |

## Migration / Rollout

No migration de runtime. El rollout debe ser en 3 slices:

1. generation model move
2. study session model
3. flashcard slimming + adapters finales

## Open Questions

- [ ] Si `GeneratedStudyCard` pertenece a `generation` o si `study` necesita su propio tipo derivado.
- [ ] Si `FlashcardReview` debe moverse a `study` en esta fase o quedar shared por ahora.
