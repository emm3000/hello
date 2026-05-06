# AGENTS.md

| Field | Value |
|---|---|
| Status | Active |
| Role | Guía operativa para agentes |
| Source of Truth | Yes |
| Read this when | Vas a editar código o documentación del repo |

## Estado obligatorio

El producto corre en modo local-first single-device.

No asumir:

- sync remoto activo
- pairing activo
- bootstrap remoto activo

## Fuentes a leer

1. `README.md`
2. `ARCHITECTURE.md`
3. `LOCAL_FIRST.md`
4. `docs/README.md`

## Reglas del repo

- módulos: `:app`, `:data`, `:domain`
- dependencias: `app -> data`, `app -> domain`, `data -> domain`
- `domain` se mantiene JVM-only
- `HelloDb` es source of truth

## Startup actual

- `App -> Koin -> AppStartupCoordinator.start()`
- startup solo inicializa identidad local de instalación

## Convenciones de features

- MVI por feature con `UiState`, `UiIntent`, `UiEffect`
- punto de entrada público: `onIntent(intent)`
- naming en `app/src/main/kotlin/com/emm/hello/newfeatures/`: `*ViewModel`, `*Route`, `*UiState`, `*UiIntent`, `*UiEffect`

## Toolchain actual

- Java 17
- AGP `9.2.0`
- Kotlin `2.3.21`

## Regla final

Si una doc contradice el código actual, manda el código y luego se actualiza la doc.
