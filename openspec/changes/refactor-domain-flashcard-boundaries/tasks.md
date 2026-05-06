# Tasks: Refactor domain flashcard boundaries

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 250-450 |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 generation+authoring move -> PR 2 study move -> PR 3 cleanup |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Separar `generation` y `authoring` | PR 1 | Incluye tests/imports del pipeline de create |
| 2 | Alinear ownership de `study` | PR 2 | Mueve sesión de estudio sin tocar scheduling |
| 3 | Cleanup de naming y docs del cambio | PR 3 | Solo si sigue siendo necesario |

## Phase 1: Foundation

- [ ] 1.1 Crear packages `com.emm.domain.generation` y `com.emm.domain.authoring` sin cambiar firmas públicas.
- [ ] 1.2 Mover `ValidateGeneratedLearningNoteUseCase` y policies relacionadas a `generation`.
- [ ] 1.3 Mover `CreateFlashcardUseCase` y `GeneratedLearningNoteMapper` a `authoring`.

## Phase 2: Study alignment

- [ ] 2.1 Mover `GetStudySessionUseCase` a `domain/src/main/kotlin/com/emm/domain/study/`.
- [ ] 2.2 Revisar `StudySessionRepository` y tipos asociados para que sigan describiendo estudio.
- [ ] 2.3 Mantener `ScheduleFlashcardReviewUseCase` y `SpacedRepetitionScheduler` sin cambios de comportamiento.

## Phase 3: Wiring

- [ ] 3.1 Actualizar imports y wiring en `app/src/main/kotlin/com/emm/hello/` para el nuevo layout.
- [ ] 3.2 Actualizar imports y repositorios en `data/src/main/kotlin/com/emm/data/` sin cambiar contratos.
- [ ] 3.3 Verificar que `Flashcard.kt` siga estable en esta fase y no absorber más cambios de modelo.

## Phase 4: Tests

- [ ] 4.1 Actualizar `domain/src/test/kotlin/com/emm/domain/flashcard/CreateFlashcardUseCaseTest.kt` preservando escenarios de validación, deduplicación y persistencia.
- [ ] 4.2 Actualizar `domain/src/test/kotlin/com/emm/domain/study/ScheduleFlashcardReviewUseCaseTest.kt` preservando invariantes de scheduling.
- [ ] 4.3 Agregar o ajustar tests mínimos para cubrir que `GetStudySessionUseCase` siga describiendo estudio tras el move.

## Phase 5: Cleanup

- [ ] 5.1 Eliminar imports obsoletos y referencias residuales al layout viejo.
- [ ] 5.2 Actualizar docs del cambio si el shape final difiere del diseño.
