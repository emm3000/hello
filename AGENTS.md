# AGENTS.md

Guia operativa principal para agentes en este repo.

**Estado actual obligatorio**: sync remoto, pairing y bootstrap remoto fueron removidos del MVP trunk. El producto corre en modo local-only.

## SoT tecnica
- `ARCHITECTURE.md`: arquitectura vigente y boundaries.
- `LOCAL_FIRST.md`: contrato local-only y comportamiento de runtime.

## Reglas operativas
- Repo modular: `:app`, `:data`, `:domain`.
- Direccion de dependencias: `app -> data`, `app -> domain`, `data -> domain`.
- `domain` se mantiene JVM-only, sin Android/DB/network.
- `HelloDb` (SQLDelight) es source of truth de lectura y escritura.

## Startup actual (producto)
- Flujo: `App -> Koin -> AppStartupCoordinator.start()`.
- En MVP actual solo se inicializa identidad local de instalacion.
- No hay runtime activo de sync, push/pull, ack, pairing ni workers remotos.

## Convenciones de features
- MVI por feature: `UiState`, `UiIntent`, `UiEffect` y unico `onIntent(intent)`.
- Mantener naming en `app/src/main/kotlin/com/emm/hello/newfeatures/`: `*ViewModel`, `*Route`, `*UiState`, `*UiIntent`, `*UiEffect`.

## Verificacion (cuando aplique)
- Secuencia CI para cambios no triviales: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, `./gradlew detekt`.
- `connectedDebugAndroidTest` requiere dispositivo/emulador; no es default.

## Notas de config
- Toolchain: Java 17, AGP `9.2.0`, Kotlin `2.3.21`.
- `app/google-services.json` puede ser dummy en CI.
- Entradas Supabase en properties se consideran **legacy/archived, no activo en producto MVP**.
