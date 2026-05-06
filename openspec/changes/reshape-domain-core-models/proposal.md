# Proposal: Reshape domain core models

Rehacer el núcleo del dominio para que `generation`, `authoring` y `study` dependan de modelos propios y no de un `Flashcard` sobrecargado ni de un `GeneratedLearningNote` viviendo en el paquete incorrecto.

## Quick path

1. Sacar `GeneratedLearningNote` del paraguas `flashcard`.
2. Introducir un modelo propio de `study` en vez de usar `Flashcard` completo.
3. Reducir el peso conceptual de `Flashcard` al agregado persistido.

## Scope

### In Scope
- Mover `GeneratedLearningNote` y tipos claramente generativos a `generation`.
- Introducir un modelo de sesión de estudio separado del agregado `Flashcard`.
- Reducir campos no esenciales de `Flashcard` hacia submodelos o boundaries explícitos.

### Out of Scope
- Cambios de UX o runtime.
- Cambios de DB que no sean necesarios para soportar el nuevo shape.
- Rehacer todo `FlashcardReview` en esta misma fase si no mejora el corte.

## Capabilities

### New Capabilities
- `domain-models`: define los modelos y boundaries internos del dominio para generation, authoring, persisted flashcard y study session.

### Modified Capabilities
None.

## Approach

Evolucionar desde el refactor por ownership a un refactor de modelos. La estrategia es separar primero `GeneratedLearningNote` del agregado persistido, luego darle a `study` un contrato que no dependa del `Flashcard` completo, y por último adelgazar `Flashcard` para que represente persistencia/authoring en vez de semántica universal.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/src/main/kotlin/com/emm/domain/generation/` | Modified | Absorber note y tipos de generación |
| `domain/src/main/kotlin/com/emm/domain/flashcard/` | Modified | Reducir el agregado persistido y sus contratos |
| `domain/src/main/kotlin/com/emm/domain/study/` | Modified | Crear modelo/contrato de sesión propio |
| `data/src/main/kotlin/com/emm/data/flashcard/` | Modified | Adaptar mappers y repositorios al nuevo shape |
| `app/src/main/kotlin/com/emm/hello/newfeatures/study/` | Modified | Consumir modelo de estudio específico |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Mezclar refactor de modelos con reglas de negocio | Medium | Mantener invariantes observables con specs y tests |
| Cambiar demasiados contratos a la vez | High | Implementar por slices: generation model -> study model -> flashcard slimming |

## Rollback Plan

Revertir por slice. Cada work unit debe dejar adapters en los boundaries para volver al contrato anterior sin migración irreversible.

## Dependencies

- `openspec/changes/refactor-domain-flashcard-boundaries/*`
- `domain/src/main/kotlin/com/emm/domain/flashcard/Flashcard.kt`
- `domain/src/main/kotlin/com/emm/domain/flashcard/GeneratedLearningNote.kt`

## Success Criteria

- [ ] `GeneratedLearningNote` ya no vive bajo `flashcard`.
- [ ] `study` deja de depender de `List<Flashcard>` como contrato principal de sesión.
- [ ] `Flashcard` representa persistencia/authoring y no una mezcla de subdominios.
- [ ] El dominio queda con menos semántica cruzada y menos necesidad de helpers ambiguos.
