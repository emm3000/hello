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

### Convención de naming

- **Composables públicos**: prefijo `H` (`HInput`, `HButton`, `HBadge`, `HCard`, etc.). Esta es la regla dura.
- **Archivos**: actualmente inconsistente — la mayoría sin prefijo (`Button.kt`, `Input.kt`, `Badge.kt`, `Card.kt`) y 4 con prefijo (`HSearchBar.kt`, `HTagChip.kt`, `HTagInput.kt`, `HLoadingSpinner.kt`). Para archivos nuevos preferir **sin prefijo** (alinea con la mayoría y con el template `Input.kt`).
- **Excepción sin composable `H*`**: `FieldShell` — building block interno, template para inputs.

### Jerarquía para nuevos diseños

1. **Primero**: revisá `app/src/main/kotlin/com/emm/hello/core/ui/`. Si existe el componente que necesitás, USALO sin excepción. Los componentes están inspirados en shadcn/ui y definen el tema de la app.

2. **Local vs compartido**:
   - scope = una sola pantalla → crear en el feature (`newfeatures/X/`)
   - scope = app-wide → crear en `core/ui/`

3. **Si no existe**:
   - Crear composable `H*` en `core/ui/` (`HSearchBar`, `HChip`, `HTagInput`, etc.).
   - **NO usar** componentes raw de Material3 (`OutlinedTextField`, `Button`, `TextField`, etc.).
   - Template: `Input.kt` + `FieldShell.kt` (borde animado, fondo transparente, 48dp altura mínima).
   - Incluir preview `PreviewLightDark`.

**Regla de hierro**: un componente custom en un screen NUNCA reemplaza un componente de `core/ui` que exista para ese propósito. Si el de `core/ui` no sirve, se extiende o modifica primero.

## Linting y code style (detekt)

Reglas activas en `config/detekt/detekt.yml`:

### Complexity — evitar callback hell y anidamiento profundo

```yaml
CyclomaticComplexMethod:
  active: true
  threshold: 10
  ignoreSingleWhenExpression: true
  ignoreSimpleWhenEntries: true
  nestingFunctions:
    - 'also'
    - 'apply'
    - 'run'
    - 'let'
    - 'use'
    - 'with'
```

**Por qué**: Estas funciones son las que generan callback hell. Si ves métodos con muchos `also { apply { run { ... } } }`, refactorear con funciones intermedias o early return.

### Style — early return y returns moderados

```yaml
ReturnCount:
  active: true
  max: 5
  excludeLabeled: true       # labeled returns no cuentan (return@mapNotNull)
  excludedFunctions:
    - 'equals'
  ignoreAnnotated:
    - 'Composable'
```

**Por qué**: Más de 5 returns confunde. Usar:
- Early returns en guard clauses (validación, null-checks)
- Labeled returns en lambdas (`return@mapNotNull null`) para short-circuit
- Extraer lógica a funciones privadas si un método tiene muchos branches

### Regla de hierro para código nuevo

Antes de commitear, verificar:
1. ¿Hay más de 3 niveles de nesting? → extraer función
2. ¿Hay muchos `else if` encadenados? → usar `when` o extraer funciones
3. ¿La función hace muchas cosas? → split en funciones más pequeñas
4. ¿Las lambdas tienen `also/apply/run/let` anidados? → refactorizar con funciones intermedias

## Toolchain actual

- Java 17
- AGP `9.2.0`
- Kotlin `2.3.21`

## Regla final

Si una doc contradice el código actual, manda el código y luego se actualiza la doc.
