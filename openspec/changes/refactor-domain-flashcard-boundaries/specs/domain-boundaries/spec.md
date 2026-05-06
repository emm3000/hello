# domain-boundaries Specification

## Purpose

Definir los invariantes estructurales que el refactor del dominio DEBE preservar mientras separa ownership entre `generation`, `authoring` y `study`.

## Requirements

### Requirement: Ownership boundaries MUST match domain intent

El dominio MUST ubicar tipos, contratos y casos de uso bajo el contexto conceptual que describen. `generation`, `authoring` y `study` SHALL tener ownership explícito y no compartir el mismo paquete solo por historia.

#### Scenario: Study use case moves under study

- GIVEN un caso de uso cuyo lenguaje y dependencia expresan estudio
- WHEN se reorganiza el paquete del dominio
- THEN ese caso de uso queda bajo `study`
- AND no permanece bajo `flashcard` por conveniencia histórica

#### Scenario: Edge case for mixed orchestration

- GIVEN un tipo que coordina más de una responsabilidad conceptual
- WHEN el refactor evalúa su ubicación
- THEN el ownership se define por la responsabilidad dominante
- AND las responsabilidades auxiliares quedan delegadas a collaborators explícitos

### Requirement: Generated note boundary MUST remain explicit

El dominio MUST preservar una frontera explícita entre `GeneratedLearningNote` y la `Flashcard` persistida. Ningún refactor SHALL colapsar ambos modelos en un único contrato implícito.

#### Scenario: Create pipeline keeps mapping boundary

- GIVEN una `GeneratedLearningNote` válida
- WHEN el pipeline de creación persiste una flashcard
- THEN existe un paso explícito de mapping hacia el input persistido
- AND la nota generada no se trata como entidad persistida directa

#### Scenario: Invalid generated note stays rejected before persistence

- GIVEN una `GeneratedLearningNote` inválida
- WHEN `CreateFlashcardUseCase` ejecuta el pipeline
- THEN la validación falla antes de crear la flashcard
- AND no se alcanza persistencia ni upsert de examples

### Requirement: Study contracts MUST stay cohesive

Los casos de uso y contratos de estudio MUST permanecer cohesionados alrededor de `study`. El refactor SHALL evitar que la lógica o contratos de sesión de estudio dependan de ownership de authoring.

#### Scenario: Study scheduling remains isolated

- GIVEN `ScheduleFlashcardReviewUseCase`
- WHEN se reorganiza el dominio
- THEN su comportamiento y dependencias de estudio permanecen en `study`
- AND no adquiere dependencias de generación o authoring

#### Scenario: Session retrieval aligns with study

- GIVEN recuperación de sesión diaria por deck
- WHEN el caso de uso se reubica
- THEN el contrato sigue describiendo estudio
- AND su package refleja ese lenguaje

### Requirement: Observable behavior MUST remain unchanged

El refactor MUST preservar los contratos observables ya defendidos por tests del dominio. Cambios de paquete, naming o ownership SHALL NOT alterar reglas de validación, deduplicación, persistencia ni scheduling.

#### Scenario: Create flow preserves current contract

- GIVEN una `GeneratedLearningNote` válida y no duplicada
- WHEN se ejecuta creación después del refactor
- THEN se mantiene una creación y un upsert de examples
- AND la flashcard resultante sigue releyéndose desde el repositorio

#### Scenario: Review scheduling keeps invariants

- GIVEN una review existente y un grade válido
- WHEN se ejecuta scheduling después del refactor
- THEN `nextReviewAt` sigue siendo posterior a `lastReviewedAt`
- AND las reglas de interval/ease no cambian por mover paquetes
