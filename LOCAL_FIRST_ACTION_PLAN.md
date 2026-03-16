# Plan de Accion Local-First Multi-Device con Supabase

## Supuestos de este plan

- `Hello` seguira usando `SQLDelight` como base local principal.
- `Supabase` sera el backend principal.
- no habra `login/register` tradicional en esta etapa.
- se usara `Supabase Auth anonymous` para autenticar la instalacion.
- la identidad compartida del producto sera `app_account`, no `auth.users`.
- el linking multi-device inicial se hara con `codigo corto` de 6 caracteres.
- se permite reestructurar por completo la base local y remota.

## Objetivo de entrega

Al terminar este plan, la app deberia:

- funcionar siempre contra SQLite local
- sincronizar cambios entre dispositivos de la misma cuenta
- soportar pairing de un segundo dispositivo sin email ni password
- replicar `Deck`, `Flashcard`, `FlashcardExample`, `Quote` y `ReviewEvent`
- dejar `ReviewProjection` como estado derivado local

## Fase 0. Cerrar decisiones base

- [x] Definir que `multi-device` en esta etapa significa: varios dispositivos del mismo usuario vinculados por `pairing`, no cuentas compartidas entre personas.
- [x] Fijar `pairing` v1 con codigo corto de 6 caracteres.
- [x] Confirmar si `Quote` debe sincronizarse entre dispositivos o si puede seguir siendo local por ahora.
- [x] Confirmar si la cuenta anonima debe sobrevivir reinstalacion solo con pairing o si habra restore manual.
- [x] Congelar los nombres de entidades remotas: `deck`, `flashcard`, `flashcard_example`, `review_event`, `quote`.

### Decisiones cerradas (Fase 0)

- `multi-device` v1: solo multiples dispositivos de la misma persona/cuenta (`app_account`), vinculados por pairing.
- `pairing` v1: codigo corto de 6 caracteres, de un solo uso y con expiracion corta.
- `Quote`: se sincroniza entre dispositivos en esta etapa (queda dentro del alcance del sync principal).
- Supervivencia tras reinstalacion: no hay recuperacion automatica por `auth anonymous`; se soporta recuperacion por `pairing` y, opcionalmente, restore manual si existe backup.
- Nombres remotos congelados para esta etapa: `deck`, `flashcard`, `flashcard_example`, `review_event`, `quote`.

### Pre-Fase 1: analisis tecnico de DB local (2026-03-15)

- Informe: `LOCAL_DB_ANALYSIS_2026-03-15.md`.
- Riesgos prioritarios detectados antes de migrar:
  - mezcla de unidades de tiempo en `FlashcardReview` (segundos/milisegundos)
  - `FlashcardExample.flashcardId` nullable con riesgo de huerfanos
  - uso de `INSERT OR REPLACE` en entidades principales
  - falta de indices en consultas frecuentes de lectura y sync
- Convencion propuesta:
  - remoto en `snake_case` (ya congelado en Fase 0)
  - local: decidir `snake_case` o mantener `camelCase` y mapear en una sola capa
- [x] Cerrar decision final de unidad temporal (`epoch_millis` recomendado).
- [x] Cerrar decision final de convencion de nombres local (`snake_case` o `camelCase` + mapper unico).

Decisiones cerradas:

- Unidad temporal local: `epoch_millis` en todos los timestamps de dominio/scheduler.
- Convencion de nombres local: `camelCase` (alineado a Kotlin/SQLDelight actual) y mapeo remoto centralizado a `snake_case`.

## Fase 1. Reiniciar el modelo local

- [x] Eliminar la dependencia conceptual de `syncStatus = Pending/Synced`.
- [x] Crear tabla local `LocalDeviceIdentity`.
- [x] Crear tabla local `LocalAccountState`.
- [x] Crear tabla local `OperationLog`.
- [x] Crear tabla local `SyncCheckpoint`.
- [x] Crear tabla local `DeadLetterOperation`.
- [x] Crear tabla local `ReviewEvent`.
- [x] Crear tabla local `ReviewProjection`.
- [x] Agregar `deletedAt` o `isDeleted` a `Deck`.
- [x] Agregar `deletedAt` o `isDeleted` a `Flashcard`.
- [x] Agregar `deletedAt` o `isDeleted` a `FlashcardExample`.
- [x] Agregar `deletedAt` o `isDeleted` a `Quote`.
- [x] Agregar `originDeviceId` a entidades sincronizables.
- [x] Agregar `lastModifiedByDeviceId` a entidades sincronizables.
- [x] Agregar `versionLamport` a entidades sincronizables.
- [x] Eliminar el uso de `androidId` como identidad principal de sync.

## Fase 2. Rediseñar el dominio local

- [x] Reemplazar la idea de "review sincronizable" por `ReviewEvent` como evento fuente.
- [x] Mantener `ReviewProjection` como lectura rapida para UI.
- [x] Definir `sealed class SyncOperationPayload` en Kotlin para todas las operaciones replicables.
- [x] Definir un `OperationType` comun: `Create`, `Update`, `Delete`, `AppendEvent`.
- [x] Definir una sola abstraccion `SyncEngine` en `domain` o `data`.
- [x] Definir un `SyncState` observable para UI y debug.
- [x] Marcar los repositorios para que toda escritura sea local primero.

## Fase 3. Escribir local + outbox en una sola transaccion

- [x] Refactorizar `DefaultDeckRepository` para escribir `Deck` y `OperationLog` en una misma transaccion.
- [x] Refactorizar `DefaultFlashcardRepository.create()` para escribir `Flashcard` y `OperationLog`.
- [x] Refactorizar `DefaultFlashcardRepository.upsertExamples()` para escribir `FlashcardExample` y `OperationLog`.
- [x] Refactorizar `DefaultQuoteRepository.generate()` para escribir `Quote` y `OperationLog`.
- [x] Refactorizar `DefaultFlashcardReviewRepository.update()` para crear `ReviewEvent` y recalcular `ReviewProjection`.
- [x] Eliminar la necesidad de disparar sync desde cada repositorio inmediatamente despues de escribir.
- [x] Agregar incremento monotono local de `lamport` por dispositivo.
- [x] Guardar cada operacion con `opId`, `entityId`, `payload`, `lamport`, `createdAt`, `status`.

## Fase 4. Limpiar la estrategia actual de sync

- [x] Desacoplar `DeckSynchronizer` de `Context` y `WorkManager`.
- [x] Desacoplar `FlashcardSynchronizer` de `Context` y `WorkManager`.
- [x] Desacoplar `FlashcardReviewSynchronizer` de `Context` y `WorkManager`.
- [x] Desacoplar `QuoteSynchronizer` de `Context` y `WorkManager`.
- [x] Retirar gradualmente los workers por entidad como camino principal.
- [x] Mantener un solo `SyncWorker` o `SyncEngineWorker` para drenar la outbox y hacer pull.
- [x] Separar por completo `backup` de `sync`.

## Fase 5. Preparar Supabase

- [x] Crear proyecto de Supabase para desarrollo.
- [x] Habilitar `Anonymous Sign-Ins` en Supabase Auth.
- [x] Configurar `supabase-kt` en Android para `Auth`, `Postgrest`, `Functions`, `Realtime`.
- [x] Crear script SQL versionado para bootstrap de schema remoto.
- [x] Crear tabla `app_account`.
- [x] Crear tabla `app_device`.
- [x] Crear tabla `pairing_session`.
- [x] Crear tabla `sync_operation`.
- [x] Crear tabla `sync_ack`.
- [x] Crear tabla `sync_cursor` si decides persistirlo del lado servidor.
- [x] Crear tabla remota `deck`.
- [x] Crear tabla remota `flashcard`.
- [x] Crear tabla remota `flashcard_example`.
- [x] Crear tabla remota `review_event`.
- [x] Crear tabla remota `quote`.
- [x] Agregar `app_account_id` a todas las tablas de dominio remotas.
- [x] Crear indices por `app_account_id`, `entity_id`, `lamport`, `created_at`.

## Fase 6. Seguridad y acceso

- [x] Crear politicas RLS para `app_device`.
- [x] Crear politicas RLS para tablas de dominio basadas en pertenencia a `app_account`.
- [x] Crear politicas RLS para `sync_operation`.
- [x] Definir si el cliente tendra acceso directo a tablas de dominio o solo a `RPC`.
- [x] Si eliges solo `RPC` para sync, minimizar acceso directo desde cliente a tablas sensibles.
- [x] Agregar revocacion de dispositivos en `app_device`.

## Fase 7. Protocolo de sync en Supabase

- [x] Diseñar el contrato de `sync_push`.
- [x] Diseñar el contrato de `sync_pull`.
- [x] Diseñar el contrato de `sync_ack`.
- [x] Implementar `rpc.sync_push(batch jsonb)`.
- [x] Implementar `rpc.sync_pull(cursor bigint, limit int)`.
- [x] Implementar `rpc.sync_ack(op_ids jsonb)`.
- [x] Hacer que `sync_push` aplique operaciones idempotentemente.
- [x] Hacer que `sync_push` escriba `sync_operation` con secuencia global o por cuenta.
- [x] Hacer que `sync_pull` entregue operaciones ordenadas por cursor.
- [x] Hacer que `sync_pull` excluya operaciones originadas por el mismo dispositivo si asi lo decides.
- [x] Hacer que `sync_push` devuelva `acks` y metadata canonica.
- [x] Agregar tests SQL o integration tests para idempotencia y orden.

## Fase 8. Pairing multi-device

- [x] Implementar `create_pairing_session`.
- [x] Implementar expiracion corta de pairing code.
- [x] Implementar `redeem_pairing_code`.
- [x] Asociar el nuevo `auth_user_id` anonimo al mismo `app_account_id`.
- [x] Registrar el nuevo `app_device`.
- [x] Bloquear reutilizacion de pairing code.
- [x] Crear pantalla simple de `Link new device`.
- [x] Crear pantalla simple de `Join device`.
- [x] Permitir mostrar lista de dispositivos vinculados.
- [x] Permitir revocar un dispositivo desde otro dispositivo autorizado.

## Fase 9. Android SyncEngine

- [x] Crear caso de uso `DrainOutbox`.
- [x] Crear caso de uso `PullRemoteOperations`.
- [x] Crear caso de uso `ApplyRemoteOperation`.
- [x] Crear caso de uso `AckOperations`.
- [x] Implementar `SyncEngine.runOnce()`.
- [x] Implementar backoff exponencial para errores recuperables.
- [x] Implementar reintento inmediato en cambio de conectividad.
- [x] Persistir `lastPulledCursor` en SQLite.
- [x] Persistir `lastSuccessfulSyncAt`.
- [x] Persistir `lastSyncError`.
- [x] Exponer `SyncState` como `Flow`.

## Fase 10. Integrar WorkManager sin acoplar dominio

- [x] Crear un solo worker `SyncEngineWorker`.
- [x] Hacer que el worker solo invoque `SyncEngine.runOnce()`.
- [x] Programar sync periodico moderado.
- [x] Programar sync one-shot cuando haya nuevas operaciones pendientes.
- [x] Cancelar o deprecar los workers antiguos por entidad.
- [x] Mantener foreground sync solo si realmente hace falta por duracion.

## Fase 11. Aplicacion de operaciones remotas

- [x] Implementar merge de `Deck` por campo con `versionLamport`.
- [x] Implementar merge de `Flashcard` por campo con `versionLamport`.
- [x] Implementar merge de `FlashcardExample` por entidad hija.
- [x] Implementar merge de `Quote`.
- [x] Implementar `Delete` con `deletedAt` o tombstones.
- [x] Implementar aplicacion de `ReviewEvent`.
- [x] Recalcular `ReviewProjection` cada vez que entra un `ReviewEvent`.
- [x] Garantizar idempotencia de aplicacion local por `opId`.

## Fase 12. Realtime como acelerador

- [ ] Suscribirse a una señal ligera de cambios por cuenta o por dispositivo.
- [ ] Cuando llegue una señal, disparar `sync_pull`.
- [ ] No aplicar cambios directamente desde `Realtime` a las tablas locales sin pasar por el pipeline de merge.
- [ ] Medir si `Realtime` realmente aporta valor o si polling con cursor es suficiente.

## Fase 13. UI y observabilidad

- [x] Mostrar `pending operations` en pantalla de debug.
- [x] Mostrar `last successful sync`.
- [x] Mostrar `last sync error`.
- [x] Mostrar `device id` y `app account id` en pantalla de debug.
- [x] Mostrar lista de dispositivos vinculados.
- [ ] Agregar logging estructurado de sync en Android.
- [ ] Agregar tabla o vista de auditoria de operaciones en Supabase para soporte.

## Fase 14. Corte con el sistema anterior

- [x] Eliminar dependencias de `syncStatus` donde ya no sean necesarias.
- [x] Eliminar llamadas a endpoints legacy `/hello`, `/decks/all`, `/flashcards/all`, `/examples/all`, `/reviews/all`, `/quotes/all` cuando el nuevo pipeline ya cubra esos casos.
- [x] Retirar `RemoteDataSource` legacy o reconvertirlo a cliente del nuevo `SyncApi`.
- [x] Retirar `WorkManagerSyncManager` basado en nombre de work legacy si deja de servir.
- [x] Dejar `backup` como feature separada, no como sync principal.

## Fase 15. Validacion de escenarios reales

Checklist operativo recomendado: `PHASE15_MULTI_ENTITY_2DEVICES_VALIDATION_CHECKLIST.md`

- [ ] Probar alta de deck en dispositivo A y replica en dispositivo B.
- [ ] Probar alta de flashcard en dispositivo A y replica en dispositivo B.
- [ ] Probar review en A y continuidad en B.
- [ ] Probar cambios concurrentes del mismo `Flashcard` en A y B.
- [ ] Probar delete en A y no resurreccion en B.
- [ ] Probar linking de un segundo dispositivo con pairing code expirado.
- [ ] Probar revocacion de dispositivo.
- [ ] Probar cold start sin red.
- [ ] Probar red intermitente con operaciones pendientes.

## Orden recomendado de ejecucion

Si quieres avanzar sin dispersarte, este es el orden correcto:

1. Fase 0
2. Fase 1
3. Fase 2
4. Fase 3
5. Fase 5
6. Fase 6
7. Fase 7
8. Fase 9
9. Fase 11
10. Fase 8
11. Fase 10
12. Fase 12
13. Fase 13
14. Fase 14
15. Fase 15

## Primer sprint recomendado

Si tuvieras que empezar ya, yo haria solo esto:

- [ ] Crear nuevo schema local: `OperationLog`, `SyncCheckpoint`, `ReviewEvent`, `ReviewProjection`, `LocalDeviceIdentity`, `LocalAccountState`.
- [ ] Refactorizar un solo flujo end-to-end: `Deck`.
- [x] Montar Supabase con `Anonymous Auth`, `app_account`, `app_device`, `sync_operation`.
- [x] Implementar `sync_push` y `sync_pull` solo para `Deck`.
- [x] Implementar pairing minimo con codigo corto de 6 caracteres.
- [x] Probar dos dispositivos reales con `Deck` antes de tocar `FlashcardReview`.

## Decision ya tomada

- `Pairing` v1 sera con codigo corto de 6 caracteres.
- `QR` queda como mejora futura de UX, no como requisito inicial.
- `Sync` v1 usara `RPC` como camino principal y se minimiza escritura directa del cliente en tablas sensibles.
