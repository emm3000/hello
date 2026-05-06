# domain-models Specification

## Purpose

Definir los invariantes del siguiente refactor del dominio para separar los modelos de generación, persistencia y estudio sin cambiar el comportamiento observable del producto.

## Requirements

### Requirement: Generation model MUST own generated note semantics

El dominio MUST ubicar `GeneratedLearningNote` y los tipos que solo existen para generación bajo `generation`. `flashcard` SHALL NOT seguir siendo dueño del modelo de nota generada.

#### Scenario: Generated note moves under generation

- GIVEN un modelo usado para validación, regeneración y mapping de preview
- WHEN se reestructura el dominio
- THEN ese modelo vive bajo `generation`
- AND `authoring` lo consume como boundary explícito

#### Scenario: Generated note keeps semantic guards

- GIVEN una `GeneratedLearningNote`
- WHEN `authoring` requiere expression o meaning válidos
- THEN la nota sigue ofreciendo guards semánticos equivalentes
- AND el move no degrada validación ni invariantes actuales

### Requirement: Study session MUST use a study-specific model

`study` MUST exponer un contrato de sesión propio y SHALL NOT depender del agregado `Flashcard` completo como shape principal de sesión.

#### Scenario: Session retrieval returns study-specific contract

- GIVEN una sesión diaria por deck
- WHEN el dominio entrega items de estudio
- THEN el contrato representa solo datos necesarios para study
- AND no arrastra metadata de authoring irrelevante

#### Scenario: Study scheduling keeps review invariants

- GIVEN una review previa y una nota final de sesión
- WHEN se agenda la próxima review
- THEN se preservan `nextReviewAt`, interval y easeFactor
- AND el nuevo contrato de sesión no altera esas reglas

### Requirement: Persisted flashcard MUST stay focused

`Flashcard` MUST representar el agregado persistido y SHOULD dejar de cargar semántica generativa o de sesión cuando existan modelos más específicos.

#### Scenario: Persisted flashcard keeps authoring fields

- GIVEN la entidad persistida usada por authoring y detalle
- WHEN se adelgaza el modelo
- THEN conserva solo campos necesarios para persistencia y lectura de flashcard
- AND la semántica generativa se mueve a un boundary separado

#### Scenario: Edge case for mixed consumers

- GIVEN un consumidor que hoy usa `Flashcard` por conveniencia
- WHEN el modelo se separa
- THEN consume un adapter o contrato específico
- AND no fuerza a `Flashcard` a seguir siendo modelo universal

### Requirement: Refactor MUST reduce accidental complexity

El refactor MUST reducir helpers ambiguos, extensiones multipropósito y archivos que mezclan demasiadas responsabilidades conceptuales.

#### Scenario: Mapping stays explicit instead of scattered

- GIVEN una transición entre note generada, agregado persistido y sesión de estudio
- WHEN se implementa el nuevo shape
- THEN los mappers o adapters quedan en boundaries claros
- AND no se reparten como extensiones ambiguas en archivos generales

#### Scenario: Behavior remains unchanged during transition

- GIVEN creación, preview y estudio ya implementados
- WHEN se aplican los slices del refactor
- THEN el comportamiento observable sigue igual
- AND los tests pueden validar cada boundary por separado
