# Design: Refactor domain flashcard boundaries

Este diseño implementa un refactor interno de `domain` sin tocar UX ni runtime. La estrategia es mover ownership por seams ya visibles en código y tests, manteniendo firmas públicas estables mientras cambian package names y collaborators explícitos.

## Quick path

1. Aislar `generation` alrededor de `GeneratedLearningNote` y sus policies.
2. Dejar `authoring` como pipeline note -> persistencia.
3. Mover estudio bajo `study` sin tocar reglas de scheduling.

## Architecture Decisions

| Topic | Choice | Alternatives considered | Rationale |
|-------|--------|-------------------------|-----------|
| Granularidad | Separar por paquetes, no por módulos Gradle | Crear nuevos módulos `:domain-generation` o similares | El problema hoy es ownership semántico, no acoplamiento físico entre módulos |
| Boundary note -> flashcard | Mantener mapper explícito | Persistir `GeneratedLearningNote` directamente | `GeneratedLearningNoteMapper` ya expresa una frontera real y protege el modelo persistido |
| Study ownership | Mover `GetStudySessionUseCase` a `study` | Dejarlo en `flashcard` por retorno de `Flashcard` | El lenguaje del caso de uso es de estudio; el retorno no define ownership |
| Entidad `Flashcard` | Mantenerla estable en primera fase | Partir la entidad ahora | Separar paquetes primero reduce riesgo y evita mezclar refactor estructural con remodelado de datos |

## Data Flow

```text
GeneratedLearningNote
  -> ValidateGeneratedLearningNoteUseCase
  -> EnsureUniqueFlashcardInDeckUseCase
  -> GeneratedLearningNoteMapper
  -> FlashcardWriteRepository
  -> FlashcardReadRepository
  -> Flashcard

StudySessionRepository
  -> GetStudySessionUseCase
  -> Flashcard list for study

FlashcardReview + ReviewGrade
  -> ScheduleFlashcardReviewUseCase
  -> SpacedRepetitionScheduler
```

## Target package shape

| Package | Owns |
|--------|------|
| `com.emm.domain.generation` | `GeneratedLearningNote`, validation policies, generation ports/use cases |
| `com.emm.domain.authoring` | create pipeline, duplicate checks, mapping to persistence inputs |
| `com.emm.domain.study` | session retrieval, scheduling, grades and study-centric contracts |
| `com.emm.domain.flashcard` | entidad persistida `Flashcard`, repositorios y tipos estrictamente persistidos |

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/.../CreateFlashcardUseCase.kt` | Modify/Move | Reubicar en `authoring` y mantener pipeline actual |
| `domain/.../GeneratedLearningNoteMapper.kt` | Modify/Move | Reubicar en `authoring` como frontera explícita |
| `domain/.../ValidateGeneratedLearningNoteUseCase.kt` | Modify/Move | Reubicar en `generation` con sus policies |
| `domain/.../GetStudySessionUseCase.kt` | Modify/Move | Reubicar en `study` |
| `domain/.../Flashcard.kt` | Keep | Mantener estable en fase 1 |
| `app/...` y `data/...` imports | Modify | Ajustar referencias al nuevo package layout |
| `domain/src/test/...` | Modify | Actualizar imports y preservar contratos existentes |

## Interfaces / Contracts

```kotlin
// Fase 1: no cambian firmas públicas; cambia ownership/package.
suspend operator fun invoke(deckId: DeckId, learningNote: GeneratedLearningNote): Flashcard
operator fun invoke(note: GeneratedLearningNote): ValidationResult<GeneratedLearningNote>
suspend operator fun invoke(deckId: String): List<Flashcard>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit | Contratos actuales de creación y validación | Mantener tests existentes y actualizar imports |
| Unit | Invariantes de scheduling | Mantener tests de `ScheduleFlashcardReviewUseCase` |
| Integration | Wiring `app`/`data` con nuevos packages | Compilación y ajustes de imports en fase de apply |

## Migration / Rollout

No migration required. El rollout es por slices de refactor y cada slice debe dejar el proyecto compilable.

## Open Questions

- [ ] Si `FlashcardReview` debería moverse a `study` en la misma fase o en una fase posterior.
- [ ] Si `StudySessionRepository` debe seguir devolviendo `Flashcard` o merece un modelo de sesión propio más adelante.
