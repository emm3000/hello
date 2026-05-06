# Tasks: Reshape domain core models

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-700 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 generation model -> PR 2 study model -> PR 3 flashcard slimming |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Sacar `GeneratedLearningNote` de `flashcard` | PR 1 | Incluye tipos/tests/mappers afectados |
| 2 | Crear contrato de sesión study específico | PR 2 | Incluye repo + app study wiring |
| 3 | Adelgazar `Flashcard` y adapters finales | PR 3 | Solo después de que PR1 y PR2 estabilicen |

## Phase 1: Generation model move

- [ ] 1.1 Mover `GeneratedLearningNote` a `domain/src/main/kotlin/com/emm/domain/generation/` con sus imports y tests.
- [ ] 1.2 Evaluar y mover `GeneratedNoteQualityCheck`, `GeneratedNoteQualityCode` y tipos puramente generativos al mismo paquete.
- [ ] 1.3 Ajustar `authoring` y `data` para consumir el boundary nuevo sin cambiar comportamiento.

## Phase 2: Study session model

- [ ] 2.1 Crear `StudyFlashcard` o contrato equivalente en `domain/src/main/kotlin/com/emm/domain/study/`.
- [ ] 2.2 Cambiar `StudySessionRepository` y `GetStudySessionUseCase` para devolver el contrato de `study`.
- [ ] 2.3 Adaptar `StudyViewModel` y `StudySessionItem` para consumir ese modelo sin depender de `Flashcard` completo.

## Phase 3: Flashcard slimming

- [ ] 3.1 Redefinir `domain/src/main/kotlin/com/emm/domain/flashcard/Flashcard.kt` para dejar solo semántica persistida/authoring.
- [ ] 3.2 Extraer adapters o submodelos explícitos donde hoy `Flashcard` expone campos generativos o de sesión.
- [ ] 3.3 Actualizar mappers de `data` para mapear aggregate persistido y study contract por separado.

## Phase 4: Tests and cleanup

- [ ] 4.1 Mover/alinear tests por ownership nuevo sin perder cobertura de invariantes.
- [ ] 4.2 Verificar que create flow y study flow mantengan behavior observable.
- [ ] 4.3 Eliminar extensiones/helpers ambiguos que hayan quedado sobrando tras el reshape.
