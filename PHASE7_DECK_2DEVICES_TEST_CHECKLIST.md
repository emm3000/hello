# Phase 7 Deck Sync - 2 Devices Manual Checklist

## Objetivo
Validar sync end-to-end de `Deck` entre dos dispositivos reales usando:
- `OperationLog` local
- RPC Supabase `sync_push`, `sync_pull`, `sync_ack`
- modelo remoto `deck` + `sync_operation` + `sync_ack`

## Pre-requisitos
- Ambos dispositivos usan build con commit `f7cb196` o posterior.
- Ambos dispositivos apuntan al mismo `SUPABASE_URL` y `SUPABASE_ANON_KEY`.
- Proyecto Supabase `Hello` con migraciones aplicadas.
- Internet estable para la primera corrida.

## Convención
- Dispositivo A: crea datos.
- Dispositivo B: recibe/edita datos.
- Espera máxima por sync periódico: 15 minutos (WorkManager).
- Si quieres acelerar, cierra y reabre app para forzar ejecución próxima del worker.

## Caso 1 - Bootstrap anónimo por dispositivo
1. Instalar y abrir app en A.
2. Instalar y abrir app en B.
3. Esperar primer ciclo de sync en ambos.

Resultado esperado:
- En remoto existen 2 filas en `app_device` (una por instalación).
- Cada dispositivo tiene `LocalAccountState` no vacío.

## Caso 2 - Create Deck en A, replica en B
1. En A crear un deck nuevo (nombre único, por ejemplo `deck-a-01`).
2. Esperar sync en A y luego en B.
3. Abrir lista de decks en B.

Resultado esperado:
- El deck aparece en B con mismo nombre.
- En A la operación local pasa a estado `Acked` en `OperationLog`.

## Caso 3 - Update Deck en B, replica en A
1. En B editar el deck `deck-a-01` y cambiar nombre (ejemplo `deck-a-01-edit`).
2. Esperar sync en B y luego en A.
3. Abrir lista de decks en A.

Resultado esperado:
- A muestra el nombre actualizado.
- No hay duplicados del mismo `deck.id`.

## Caso 4 - Delete lógico en A, replica en B
1. En A eliminar el deck editado.
2. Esperar sync en ambos.
3. Verificar en B que ya no se muestre en UI.

Resultado esperado:
- `deletedAt` local en B queda con valor no nulo para ese deck (si consultas DB local).
- Remoto conserva la fila con `deleted_at` no nulo.

## Caso 5 - Offline en A y reconexión
1. En A apagar internet.
2. Crear 2 decks nuevos.
3. Verificar que no llegan a B (todavía).
4. Encender internet en A.
5. Esperar sync.

Resultado esperado:
- Ambas operaciones pendientes en A se drenan.
- Los 2 decks aparecen en B.
- No se generan duplicados.

## Caso 6 - Idempotencia básica
1. Reabrir A y B varias veces durante sync (simular reintentos).
2. Esperar varios ciclos.

Resultado esperado:
- `sync_operation` no duplica un mismo `op_id` por cuenta.
- Estado local converge igual en ambos dispositivos.

## SQL de verificación rápida (Dashboard > SQL Editor)

```sql
-- 1) Dispositivos activos por cuenta
select app_account_id, count(*) as devices
from public.app_device
where revoked_at is null
group by app_account_id
order by devices desc;
```

```sql
-- 2) Últimas operaciones de sync
select cursor, app_account_id, op_id, entity_type, operation_type, lamport, created_at
from public.sync_operation
order by cursor desc
limit 50;
```

```sql
-- 3) Acks por dispositivo
select device_id, count(*) as acks
from public.sync_ack
group by device_id
order by acks desc;
```

```sql
-- 4) Estado de decks remotos (incluye borrados)
select app_account_id, id, name, deleted_at, version_lamport, updated_at
from public.deck
order by updated_at desc
limit 50;
```

## Criterio de aprobación de Phase 7 (Deck)
- Create/Update/Delete de `Deck` replica A <-> B.
- Offline enqueue + reconnect drena correctamente.
- Sin duplicados por reintento.
- `OperationLog` local converge a `Acked` para operaciones exitosas.
- `sync_operation`/`sync_ack` muestran trazabilidad consistente.

## Si algo falla
- Revisar que ambos dispositivos estén en el mismo proyecto Supabase.
- Revisar `SUPABASE_URL`/`SUPABASE_ANON_KEY`.
- Verificar que `Anonymous` siga habilitado en Auth.
- Confirmar en SQL si existen filas en `app_device` para ambos `auth_user_id`.
