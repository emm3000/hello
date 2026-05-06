# Proposal: Refactor domain flashcard boundaries

Separar ownership interno del dominio entre `generation`, `authoring` y `study` para reducir acoplamiento semántico en `domain`, sin cambiar behavior observable, UX ni runtime.

## Quick path

1. Reordenar paquetes internos de `domain` por bounded context conceptual.
2. Mantener estable el pipeline actual de creación y scheduling.
3. Verificar que el refactor no cambie contratos observables ni wiring externo.

## Scope

### In Scope
- Reorganizar paquetes y ownership dentro de `domain`.
- Mover casos de uso, modelos y contratos al contexto correcto.
- Hacer explícita la frontera entre note generada y flashcard persistida.

### Out of Scope
- Nuevos módulos Gradle.
- Cambios de reglas de negocio, DB, startup, UI o runtime.
- Cambios de schema, network o scheduling.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None.

## Approach

Usar un refactor estructural incremental por paquetes internos. `flashcard` debe dejar de ser paraguas genérico y pasar a representar persistencia/authoring donde corresponda. `study` debe absorber los casos de uso cuyo lenguaje ya expresa estudio, y `generation` debe concentrar la validación y semántica de `GeneratedLearningNote`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/src/main/kotlin/com/emm/domain/flashcard/` | Modified | Reducir superficie y dejar ownership persistido/authoring |
| `domain/src/main/kotlin/com/emm/domain/study/` | Modified | Absorber `GetStudySessionUseCase` y contratos de estudio |
| `app/src/main/kotlin/com/emm/hello/newfeatures/` | Modified | Ajustar imports sin cambiar comportamiento |
| `data/src/main/kotlin/com/emm/data/` | Modified | Ajustar imports de repositorios y wiring |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Mover tipos al paquete incorrecto | Medium | Usar seams ya visibles en casos de uso y tests |
| Romper imports o DI | Low | Mantener firmas públicas estables en cada slice |

## Rollback Plan

Revertir el refactor por slice o por commit. Como no cambia behavior ni schema, el rollback no requiere migraciones de datos.

## Dependencies

- `ARCHITECTURE.md`
- `docs/CARD_CREATION_CURRENT.md`
- `docs/STUDY_CURRENT.md`

## Success Criteria

- [ ] `domain` expresa ownership claro entre `generation`, `authoring` y `study`.
- [ ] `GetStudySessionUseCase` queda alineado con `study`.
- [ ] `CreateFlashcardUseCase` deja explícitas sus fronteras sin mezclar responsabilidades conceptuales.
- [ ] No cambia behavior observable del producto.
