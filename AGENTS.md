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

## Componentes UI compartidos (core/ui)

Todo nuevo diseño o componente debe seguir esta jerarquía:

1. **Primero**: revisá `app/src/main/kotlin/com/emm/hello/core/ui/`
   - Si existe el componente que necesitás, USALO sin excepción
   - Los componentes de `core/ui` están inspirados en shadcn/ui y definen el tema de la app

2. **Componente local vs compartido**:
   - `scope = una sola pantalla`: crear en el feature mismo (`newfeatures/X/`)
   - `scope = toda la app`: crear en `core/ui/` siguiendo los style guidelines de shadcn

3. **Si no existe el componente necesario**:
   - CREARLO en `core/ui/` con el patrón shadcn-style (`H*` prefix: `HInput`, `HButton`, etc.)
   - NO usar componentes raw de Material3 (`OutlinedTextField`, `Button`, etc.)
   - Consultar `Input.kt` como template — documenta el patrón exacto

4. **Para crear nuevo componente compartido**:
   - Crear en `core/ui/` con prefijo `H` (ej: `HSearchBar`, `H Chip`, `HTagInput`)
   - Seguir convenciones de `FieldShell.kt` y `Input.kt` (borde animado, fondo transparente, 48dp altura mínima)
   - Incluir preview `PreviewLightDark`
   - Agregar al exports de `core/ui` si corresponde

**Regla de hierro**: un componente custom en un screen NUNCA reemplaza un componente de `core/ui` que exista para ese propósito. Si el de `core/ui` no sirve, se extiende o modifica primero.

## Toolchain actual

- Java 17
- AGP `9.2.0`
- Kotlin `2.3.21`

## Regla final

Si una doc contradice el código actual, manda el código y luego se actualiza la doc.
