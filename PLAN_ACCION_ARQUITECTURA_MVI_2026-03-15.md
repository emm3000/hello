# Plan de acción progresivo

Fecha: 2026-03-15

Objetivo: llevar el proyecto desde el estado actual hacia una base consistente de Clean Architecture + MVI puro con `StateFlow` + `Channel` + `onIntent` + enforcement gradual con `detekt`, sin intentar un rewrite grande.

## Orden recomendado

1. Fijar convención.
2. Cortar dependencias de `app` hacia `data`.
3. Estandarizar MVI por feature.
4. Mover lógica de negocio a `domain`.
5. Endurecer `detekt`.
6. Agregar tests de regresión.

## Fase 0: Convención mínima

- [x] Crear un ADR corto de arquitectura en `docs/` con estas reglas: `app` no importa `data`, casos de uso terminan en `UseCase`, cada feature usa `UiState` + `UiIntent` + `UiEffect`, `MutableStateFlow` para estado, `Channel` para one-shot events y `onIntent(intent)` como única entrada pública.
- [x] Definir un template de feature con 5 archivos base: `UiState`, `UiIntent`, `UiEffect`, `ViewModel`, `Route`.
- [x] Definir criterio único para efectos efímeros: navegación, snackbars, toasts y cierre de pantalla salen por `Channel<UiEffect>`.
- [x] Actualizar `ARCHITECTURE.md` para que deje de decir MVVM y describa el estándar objetivo real.

## Fase 1: Cortar fugas de capa

- [x] Crear un caso de uso `GetSyncDebugStateUseCase`.
- [x] Mover la lectura de `HelloDb.localFirstQueries` fuera de `DashboardViewModel`.
- [x] Cambiar `DashboardViewModel` para que dependa solo de dominio.
- [x] Crear un modelo de dominio para dispositivos vinculados.
- [x] Dejar de exponer `LinkedDevice` de `data` en `PairingUiState`.
- [x] Crear un puerto de dominio para pairing.
- [x] Mover `ensureLinkedIdentity`, `redeemPairingCode`, `revokeLinkedDevice` y `listLinkedDevices` detrás de casos de uso.
- [x] Quitar `SupabaseSyncRemoteDataSource` de `PairingViewModel`.
- [x] Quitar `HelloDb` de `PairingViewModel`.
- [x] Crear un caso de uso `GetDefaultDeckUseCase`.
- [x] Crear un caso de uso `SetDefaultDeckUseCase`.
- [x] Quitar `DataStore` de `NewCardViewModel`.
- [x] Crear un caso de uso `SaveQuoteAsFlashcardUseCase`.
- [x] Quitar acceso directo a `FlashcardRepository` y `DataStore` desde `QuoteRoute`.

## Fase 2: Convención de `UseCase` en `domain`

- [x] Renombrar `DeckCreator` a `CreateDeckUseCase`.
- [x] Renombrar `DeckFetcher` a `GetDecksUseCase`.
- [x] Renombrar `DecksWithCardsProvider` a `GetDeckDetailUseCase` o `ObserveDeckWithCardsUseCase`.
- [x] Renombrar `FlashcardCreator` a `CreateFlashcardUseCase`.
- [x] Renombrar `FlashcardFetcher` a `GetStudySessionUseCase`.
- [x] Renombrar `FlashcardFinder` a `GetFlashcardByIdUseCase`.
- [x] Renombrar `FlashcardAndReviewFetcher` a `ObserveFlashcardsWithReviewUseCase`.
- [x] Renombrar `FlashcardReviewUpdater` a `UpdateFlashcardReviewUseCase`.
- [x] Renombrar `QuoteGenerator` a `GenerateQuoteUseCase`.
- [x] Renombrar `QuoteLastFetcher` a `ObserveLatestQuoteUseCase` o `GetLatestQuoteUseCase`.
- [x] Renombrar `BackupExecutor` a `RunBackupUseCase`.
- [x] Actualizar Koin en `newModule.kt` después de cada rename.
- [x] Ajustar imports y referencias antes de pasar al siguiente grupo de renombres.

## Fase 3: Estandarizar MVI

- [x] Crear contrato base para `ViewModel` MVI si el equipo quiere reutilizar helpers.
- [x] Definir snippet estándar por feature:
  - `private val _state = MutableStateFlow(UiState())`
  - `val uiState = _state.asStateFlow()`
  - `private val _effect = Channel<UiEffect>(Channel.BUFFERED)`
  - `val effect = _effect.receiveAsFlow()`
  - `fun onIntent(intent: UiIntent)`
- [x] Convertir `NewDeckViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [x] Crear `NewDeckUiEffect`.
- [x] Crear `NewDeckUiIntent`.
- [x] Renombrar `onAction` a `onIntent`.
- [x] Mover la navegación de éxito de `NewDeckScreen` a un `UiEffect` emitido por `Channel`.
- [x] Agregar `loading`, `error` y `success` explícitos al estado o efecto de `NewDeck`.
- [x] Convertir `NewCardViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [x] Crear `NewCardUiIntent`.
- [x] Crear `NewCardUiEffect` para snackbar y navegación.
- [x] Renombrar `onAction` a `onIntent`.
- [x] Sustituir `SuccessConsumed` por emisión de efecto vía `Channel` o rediseño completo de estado, pero no mezclar ambos modelos.
- [x] Convertir `StudyViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [x] Crear `StudyUiIntent`.
- [x] Renombrar `onProcess` a `onIntent`.
- [x] Crear `StudyUiEffect` para fin de sesión y salida.
- [x] Convertir `PairingViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [x] Crear `PairingUiIntent`.
- [x] Crear `PairingUiEffect` para mensajes efímeros.
- [x] Reemplazar métodos públicos como `createCode`, `joinWithCode`, `refreshDevices`, `revokeDevice` por intents.
- [x] Normalizar nombres públicos de estado a `uiState` en todos los `ViewModel`.
- [x] Normalizar nombre público de efectos a `effect` en todos los `ViewModel`.

## Fase 4: Mover lógica de negocio a `domain`

- [x] Mover `ReviewGrade` a `domain` si forma parte de la lógica de negocio.
- [x] Mover `SpacedRepetitionScheduler` a `domain`.
- [x] Crear `ScheduleFlashcardReviewUseCase`.
- [x] Hacer que `StudyViewModel` use el caso de uso en lugar del objeto en `app`.
- [x] Extraer desde `QuoteRoute` toda la lógica de crear `CreateFlashcardInput`.
- [x] Decidir si `CreateFlashcardInput` sigue siendo DTO de dominio o si conviene un comando más expresivo por caso de uso.

## Fase 5: Correcciones funcionales inmediatas

- [x] Corregir `NewDeckUiState.isValid` para que la descripción realmente sea opcional.
- [x] Evitar `onNavigateBack()` hasta confirmar creación exitosa del mazo.
- [x] Añadir feedback visible si crear mazo falla.
- [x] Reemplazar `zip` en `DeckDetailViewModel` por merge por `id`.
- [x] Añadir prueba que falle si una lista tiene más elementos que la otra.

## Fase 6: Repositorios y SOLID

- [x] Separar operaciones de generación IA fuera de `FlashcardRepository`.
- [x] Crear una interfaz de lectura de flashcards.
- [x] Crear una interfaz de escritura de flashcards.
- [x] Crear una interfaz específica para sesión de estudio.
- [x] Crear una interfaz específica para generación de contenido IA.
- [x] Actualizar casos de uso para depender de la interfaz mínima necesaria.

## Fase 7: `detekt` como enforcement real

- [x] Documentar que `./gradlew detekt` debe correr en cada PR.
- [x] Crear tarea para vaciar primero baseline de `domain` y mantenerla vacía.
- [x] Eliminar del baseline de `app` los issues de naming/filename más baratos.
- [x] Renombrar `networkModule.kt`, `newModule.kt` y `repositoryModule.kt` para cumplir naming consistente.
- [x] Eliminar del baseline de `data` los issues de `Filename` en `mappers.kt` y `providers.kt`.
- [x] Resolver los `TooGenericExceptionCaught` más críticos.
- [ ] Resolver los `LongMethod` en pantallas grandes empezando por `NewCardScreen`. (Regla temporalmente desactivada)
- [ ] Resolver los `CyclomaticComplexMethod` de UI reusable (`HButton`, `HInput`) si siguen creciendo. (Regla temporalmente desactivada)
- [x] Extraer `MagicNumber` de scheduler, workers y animaciones a constantes con nombre.
- [x] Evaluar agregar una regla custom o convención revisable para forzar `*UseCase` en `domain`.

## Fase 8: Tests de regresión

- [x] Añadir tests unitarios para `ScheduleFlashcardReviewUseCase`.
- [x] Añadir tests unitarios para `CreateDeckUseCase`.
- [x] Añadir tests unitarios para el flujo de pairing.
- [x] Añadir tests unitarios para `SaveQuoteAsFlashcardUseCase`.
- [x] Añadir tests del reducer o transición de estado en `NewDeck`.
- [x] Añadir tests del reducer o transición de estado en `NewCard`.
- [x] Añadir tests de emisión de `UiEffect` por `Channel`.
- [x] Añadir tests para merge de tarjetas + reviews por `id`.
- [x] Reemplazar los tests plantilla por pruebas reales o eliminarlos si no aportan valor.

## Primer sprint recomendado

- [x] Corregir `NewDeckUiState.isValid`.
- [x] Crear `NewDeckUiIntent`.
- [x] Crear `NewDeckUiEffect`.
- [x] Mover navegación de éxito/error de `NewDeck` a `Channel`.
- [x] Reemplazar `zip` por merge por `id` en `DeckDetailViewModel`.
- [x] Mover `SpacedRepetitionScheduler` a `domain`.
- [x] Renombrar `DeckCreator` y `DeckFetcher` a `*UseCase`.
- [x] Crear `GetDefaultDeckUseCase` y `SetDefaultDeckUseCase`.
- [x] Quitar `DataStore` de `NewCardViewModel`.
- [x] Limpiar 5 issues simples de `detekt` del baseline de `app`.

## Criterio de éxito

- Ningún `ViewModel` en `app` importa clases concretas de `data`, salvo la composición DI.
- Todos los casos de uso del dominio terminan en `UseCase`.
- Todas las features nuevas usan `UiState` + `UiIntent` + `UiEffect`.
- Todas las features nuevas exponen `uiState` por `StateFlow`.
- Todos los one-shot events salen por `Channel` y se consumen como `receiveAsFlow()`.
- Todos los `ViewModel` usan `onIntent(intent)` como entrada pública.
- `SpacedRepetitionScheduler` y reglas similares viven en `domain`.
- La baseline de `detekt` baja progresivamente en cada sprint.
- Existen tests reales sobre reglas y flujos críticos.
