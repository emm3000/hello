# Phase 15 - Multi-Entity Real-World Validation (2 Devices)

## Objetivo
Validar en dispositivos reales que el pipeline local-first (outbox + push/pull/ack + merge/idempotencia) converge correctamente para `Deck`, `Flashcard`, `FlashcardExample`, `Quote` y `ReviewEvent`.

## Pre-requisitos
- App en ambos dispositivos con commits recientes (incluyendo Fase 7, 9, 10, 11, 13 y 14).
- Ambos dispositivos apuntan al mismo proyecto Supabase.
- Proyecto con migraciones aplicadas.
- Ambos dispositivos vinculados a la misma `app_account`.
- Panel de debug de sync visible en Dashboard.

## Convención
- Dispositivo A: origen principal de cambios.
- Dispositivo B: verificación de réplica y continuidad.
- Espera normal de convergencia: segundos a pocos minutos.
- Si quieres acelerar: abrir Dashboard y esperar `pending=0` en A y luego en B.

## Caso 1 - Alta de deck en A, réplica en B
1. Crear deck en A con nombre único (`phase15-deck-a1`).
2. Esperar `pending=0` en A.
3. Verificar aparición en B.

Esperado:
- Deck visible en B con mismo nombre.
- Sin duplicados.

## Caso 2 - Alta de flashcard en A, réplica en B
1. En `phase15-deck-a1`, crear flashcard en A.
2. Esperar convergencia.
3. Abrir deck en B.

Esperado:
- Flashcard aparece en B con contenido consistente.

## Caso 3 - Review en A y continuidad en B
1. Iniciar sesión de estudio en A y calificar una tarjeta.
2. Esperar sync.
3. Abrir estudio del mismo deck en B.

Esperado:
- `nextReviewAt`/estado de repaso en B refleja evento aplicado.
- No se pierde progreso.

## Caso 4 - Conflicto concurrente sobre la misma flashcard (A y B)
1. Poner A y B online.
2. Editar el mismo campo de la misma flashcard casi al mismo tiempo (A y B).
3. Esperar convergencia en ambos.

Esperado:
- Ambos convergen al mismo resultado.
- Gana la versión con `versionLamport` más alto.

## Caso 5 - Delete en A sin resurrección en B
1. Eliminar (lógico) flashcard o quote en A.
2. Esperar convergencia.
3. Verificar en B que no reaparece tras reabrir pantallas y reintentos.

Esperado:
- `deletedAt` aplicado y estable.
- Sin resurrección del registro.

## Caso 6 - Pairing con código expirado
1. Generar código en A con TTL corto.
2. Esperar expiración.
3. Intentar vincular en B con código expirado.

Esperado:
- Operación rechazada con error claro.
- No se crea vínculo adicional.

## Caso 7 - Revocación de dispositivo
1. Desde A, revocar B en pantalla de pairing.
2. En B, intentar sync/acciones.

Esperado:
- B deja de sincronizar con la cuenta revocada.
- A mantiene operación normal.

## Caso 8 - Cold start sin red
1. Forzar modo avión en A.
2. Abrir app desde cero.
3. Crear cambios locales (deck/flashcard) sin red.

Esperado:
- App funciona con SQLite local.
- Operaciones quedan en `pending` hasta reconexión.

## Caso 9 - Red intermitente con operaciones pendientes
1. En A, generar varias operaciones.
2. Alternar red ON/OFF mientras corre sync.
3. Dejar red estable y esperar drenado.

Esperado:
- No hay duplicados por reintentos.
- Operaciones terminan `Acked` o `Dead` con trazabilidad.

## SQL de soporte (Supabase)

```sql
-- operaciones por cursor
select cursor, op_id, entity_type, entity_id, operation_type, lamport, origin_device_id, created_at
from public.sync_operation
order by cursor desc
limit 200;
```

```sql
-- acks por dispositivo
select device_id, count(*) as ack_count
from public.sync_ack
group by device_id
order by ack_count desc;
```

```sql
-- estado remoto de entidades clave
select id, name, deleted_at, version_lamport, updated_at
from public.deck
order by updated_at desc
limit 100;
```

```sql
select id, deck_id, word, deleted_at, version_lamport, updated_at
from public.flashcard
order by updated_at desc
limit 100;
```

```sql
select id, flashcard_id, deleted_at, version_lamport, updated_at
from public.flashcard_example
order by updated_at desc
limit 100;
```

```sql
select id, phrase, deleted_at, version_lamport, updated_at
from public.quote
order by updated_at desc
limit 100;
```

```sql
select id, flashcard_id, reviewed_at, next_review_at, version_lamport, created_at
from public.review_event
order by created_at desc
limit 100;
```

## Criterio de aprobación de Fase 15
- Los 9 casos pasan en 2 dispositivos reales.
- No hay divergencia persistente entre A y B.
- No hay duplicados por retries.
- Delete lógico converge sin resurrección.
- Pairing/revocación se comporta correctamente en seguridad y acceso.
