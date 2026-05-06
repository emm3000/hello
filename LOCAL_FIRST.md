# Runtime Local-First Actual

| Field | Value |
|---|---|
| Status | Active |
| Role | Contrato de runtime actual |
| Source of Truth | Yes |
| Read this when | Tocás startup, persistencia o supuestos de producto |

## Principios

- la UI lee desde `HelloDb`
- las escrituras persisten localmente
- startup no depende de servicios remotos
- el producto opera como single-device

## Startup

`AppStartupCoordinator.start()` hace solo esto:

1. asegura identidad local de instalación
2. marca la app como lista si sale bien
3. expone error local si falla

## Existe hoy

- identidad local con `deviceId`
- repositorios locales sobre SQLDelight
- generación con Firebase AI
- estudio local con `ReviewEvent` y `ReviewProjection`

## No existe en runtime activo

- bootstrap remoto
- pairing
- push/pull/ack remotos
- workers de sync remoto
- panel de debug de sync en path de producto

## Escritura local

Patrón vigente:

1. abrir transacción local
2. persistir entidades de negocio
3. renderizar desde consultas locales

## Alcance

- el producto actual es local-first single-device
- referencias legacy a sync o Supabase no forman parte del runtime vigente

## Ver también

- `ARCHITECTURE.md`
- `docs/CARD_CREATION_CURRENT.md`
- `docs/STUDY_CURRENT.md`
