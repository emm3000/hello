# Analisis DB Local (2026-03-15)

## Alcance revisado

- Schema SQLDelight actual en `data/src/main/sqldelight/com/emm/data/*.sq`.
- Repositorios y mappers que leen/escriben esas tablas.
- Flujo de estudio/review y sincronizacion legacy.
- Estado: proyecto en desarrollo, sin usuarios activos.

## Inventario actual

Tablas actuales:

- `Deck`
- `Flashcard`
- `FlashcardExample`
- `FlashcardReview`
- `Quote`
- `Tag`
- `FlashcardTag`
- `IARequest`

Relaciones actuales:

- `Flashcard.deckId -> Deck.id` (`ON DELETE CASCADE`)
- `FlashcardExample.flashcardId -> Flashcard.id` (`ON DELETE SET NULL`)
- `FlashcardReview.flashcardId -> Flashcard.id` (`ON DELETE CASCADE`)
- `FlashcardTag.flashcardId -> Flashcard.id` (`ON DELETE CASCADE`)
- `FlashcardTag.tagId -> Tag.id` (`ON DELETE CASCADE`)
- `IARequest.flashcardId -> Flashcard.id` (`ON DELETE CASCADE`)

## Hallazgos (priorizados)

### P0 - critico

1. Mezcla de unidades de tiempo en `FlashcardReview` (segundos vs milisegundos).
   - `Study` y scheduler operan con `epochSecond`.
   - Mappers/otros flujos usan default en `toEpochMilli`.
   - Esto puede romper filtros de "card due now".
   - Referencias:
     - `app/src/main/kotlin/com/emm/hello/newfeatures/study/SpacedRepetitionScheduler.kt`
     - `data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt`
     - `data/src/main/kotlin/com/emm/data/flashcard/FlashcardMappers.kt`
2. `INSERT OR REPLACE` en entidades principales.
   - `REPLACE` borra e inserta; puede perder semantica de actualizacion (metadata/versionado futuro) y complicar sync local-first.
   - Referencias:
     - `data/src/main/sqldelight/com/emm/data/Deck.sq`
     - `data/src/main/sqldelight/com/emm/data/Flashcard.sq`
     - `data/src/main/sqldelight/com/emm/data/FlashcardExample.sq`
     - `data/src/main/sqldelight/com/emm/data/FlashcardReview.sq`
     - `data/src/main/sqldelight/com/emm/data/Quotes.sq`

### P1 - alto

1. `FlashcardExample.flashcardId` permite `NULL` (`ON DELETE SET NULL`) aunque el dominio lo trata como hijo fuerte de `Flashcard`.
   - Riesgo: huerfanos sin padre y payloads remotos con ID vacio.
   - Referencias:
     - `data/src/main/sqldelight/com/emm/data/FlashcardExample.sq`
     - `data/src/main/kotlin/com/emm/data/remote/mappers.kt`
2. No hay indices explicitos para patrones de consulta frecuentes.
   - Impacta listas por deck, pendientes de sync y joins de examples.
   - Referencias:
     - `data/src/main/sqldelight/com/emm/data/Deck.sq`
     - `data/src/main/sqldelight/com/emm/data/Flashcard.sq`
     - `data/src/main/sqldelight/com/emm/data/FlashcardExample.sq`
     - `data/src/main/sqldelight/com/emm/data/FlashcardReview.sq`
     - `data/src/main/sqldelight/com/emm/data/Quotes.sq`
3. Escritura local + disparo sync no son atomicos.
   - Se escribe local y luego se llama sincronizador/worker por separado.
   - Riesgo: estados intermedios y complejidad de retries.
   - Referencias:
     - `data/src/main/kotlin/com/emm/data/deck/DefaultDeckRepository.kt`
     - `data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt`
     - `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt`

### P2 - medio

1. Tablas sin uso observable en capa app (`Tag`, `FlashcardTag`, `IARequest`).
   - Si no entran al roadmap, agregan costo cognitivo.
2. `all2` de `Quote` no ordena resultados.
   - Orden no determinista para UI en algunas ejecuciones.
   - Referencia:
     - `data/src/main/sqldelight/com/emm/data/Quotes.sq`
3. Naming mixto (`camelCase` en columnas locales, target remoto snake_case en plan).
   - Conviene definir convención fija para evitar fricción en migraciones y mapeos.

## Recomendaciones tecnicas para Fase 1

1. Estandarizar tiempo:
   - `INTEGER` en UTC `epoch_millis` para todas las columnas temporales, o `epoch_seconds` para todas.
   - Recomendado: `epoch_millis` para coherencia con el resto del schema actual.
2. Reemplazar `syncStatus` por `OperationLog` + `SyncCheckpoint` + `DeadLetterOperation`.
3. Cambiar `FlashcardExample.flashcardId` a `NOT NULL` y `ON DELETE CASCADE`.
4. Agregar soft-delete (`deletedAt`) en entidades sincronizables.
5. Agregar metadata de replicacion:
   - `originDeviceId`
   - `lastModifiedByDeviceId`
   - `versionLamport`
6. Evitar `INSERT OR REPLACE` para tablas de dominio sync.
   - Usar `INSERT ... ON CONFLICT(id) DO UPDATE SET ...`.

## Recomendaciones de performance (minimas)

Indices sugeridos en schema local actual/transicional:

- `CREATE INDEX idx_flashcard_deckId ON Flashcard(deckId);`
- `CREATE INDEX idx_flashcard_syncStatus ON Flashcard(syncStatus);`
- `CREATE INDEX idx_deck_syncStatus ON Deck(syncStatus);`
- `CREATE INDEX idx_example_flashcardId ON FlashcardExample(flashcardId);`
- `CREATE INDEX idx_example_syncStatus ON FlashcardExample(syncStatus);`
- `CREATE INDEX idx_review_nextReviewAt ON FlashcardReview(nextReviewAt);`
- `CREATE INDEX idx_review_syncStatus ON FlashcardReview(syncStatus);`
- `CREATE INDEX idx_quote_createdAt ON Quote(createdAt DESC);`
- `CREATE INDEX idx_quote_syncStatus ON Quote(syncStatus);`

Nota:
- Sin usuarios activos, es buen momento para resetear schema sin migraciones complejas.

## Convenciones de nombre propuestas

Para alinear local + remoto y facilitar RPC/sync:

- Tablas: `snake_case` singular (`deck`, `flashcard`, `flashcard_example`, `review_event`, `quote`).
- PK: `id TEXT`.
- FK: `<entidad>_id` (ej: `deck_id`, `flashcard_id`, `app_account_id`).
- Tiempos: `*_at` (`created_at`, `updated_at`, `deleted_at`) en UTC.
- Columnas de versionado sync:
  - `origin_device_id`
  - `last_modified_by_device_id`
  - `version_lamport`

Si decides mantener `camelCase` local por compatibilidad de codegen:

- congelar local en `camelCase` y centralizar mapeo a remoto `snake_case` en una sola capa, nunca disperso por repositorios.

## Conclusiones

- La base actual funciona para offline-first basico, pero no para local-first multi-device robusto.
- Hay dos ajustes urgentes antes de Fase 1 completa:
  - normalizar unidad temporal de `FlashcardReview`
  - endurecer integridad de `FlashcardExample` (evitar `NULL` huérfanos)
- Con esos puntos y el nuevo schema de outbox/checkpoints, el salto a SyncEngine unico sera mucho mas seguro.

## Estado aplicado en este corte

- Unidad temporal cerrada en `epoch_millis` para `FlashcardReview` en scheduler/UI/repositorio.
- `FlashcardExample.flashcardId` ajustado a `NOT NULL` + `ON DELETE CASCADE`.
- Indices agregados para consultas frecuentes (`pending`, joins y due-date).
- Metadata de replicacion agregada a entidades sincronizables (`deletedAt`, `originDeviceId`, `lastModifiedByDeviceId`, `versionLamport`) sin defaults legacy implícitos en schema.
- `syncStatus` eliminado del modelo local principal; la sync legacy por entidad queda desactivada en favor de migrar a `OperationLog`.
- `androidId` removido como identidad principal; se usa identidad local persistida en `LocalDeviceIdentity`.
- Fase 2 aplicada:
  - `ReviewEvent` como fuente en escritura de reviews
  - `ReviewProjection` como lectura rapida para UI/listados
  - contrato de sync en dominio (`OperationType`, `SyncOperationPayload`, `SyncState`, `SyncEngine`)
  - repositorios de escritura marcados con `@LocalFirstWrite`
- Fase 3 aplicada:
  - escrituras de `Deck`, `Flashcard`, `FlashcardExample`, `Quote` y `ReviewEvent` en transaccion local + outbox (`OperationLog`)
  - eliminados disparos de sync directo desde repositorios
  - `lamport` monotono por dispositivo desde `LocalDeviceIdentity` en cada operacion
- Fase 4 aplicada:
  - removidos sincronizadores/workers por entidad (legacy)
  - `SyncWorker` unico desacoplado de entidades, ejecutando `SyncEngine`
  - scheduling de backup separado de scheduling de sync (`BackupSync` vs `Sync`)
- Schema Fase 1 agregado en SQLDelight (`LocalFirst.sq`) con:
  - `LocalDeviceIdentity`
  - `LocalAccountState`
  - `OperationLog`
  - `SyncCheckpoint`
  - `DeadLetterOperation`
  - `ReviewEvent`
  - `ReviewProjection`
