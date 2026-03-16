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

- [ ] Crear un ADR corto de arquitectura en `docs/` con estas reglas: `app` no importa `data`, casos de uso terminan en `UseCase`, cada feature usa `UiState` + `UiIntent` + `UiEffect`, `MutableStateFlow` para estado, `Channel` para one-shot events y `onIntent(intent)` como única entrada pública.
- [ ] Definir un template de feature con 5 archivos base: `UiState`, `UiIntent`, `UiEffect`, `ViewModel`, `Route`.
- [ ] Definir criterio único para efectos efímeros: navegación, snackbars, toasts y cierre de pantalla salen por `Channel<UiEffect>`.
- [ ] Actualizar `ARCHITECTURE.md` para que deje de decir MVVM y describa el estándar objetivo real.

## Fase 1: Cortar fugas de capa

- [ ] Crear un caso de uso `GetSyncDebugStateUseCase`.
- [ ] Mover la lectura de `HelloDb.localFirstQueries` fuera de `DashboardViewModel`.
- [ ] Cambiar `DashboardViewModel` para que dependa solo de dominio.
- [ ] Crear un modelo de dominio para dispositivos vinculados.
- [ ] Dejar de exponer `LinkedDevice` de `data` en `PairingUiState`.
- [ ] Crear un puerto de dominio para pairing.
- [ ] Mover `ensureLinkedIdentity`, `redeemPairingCode`, `revokeLinkedDevice` y `listLinkedDevices` detrás de casos de uso.
- [ ] Quitar `SupabaseSyncRemoteDataSource` de `PairingViewModel`.
- [ ] Quitar `HelloDb` de `PairingViewModel`.
- [ ] Crear un caso de uso `GetDefaultDeckUseCase`.
- [ ] Crear un caso de uso `SetDefaultDeckUseCase`.
- [ ] Quitar `DataStore` de `NewCardViewModel`.
- [ ] Crear un caso de uso `SaveQuoteAsFlashcardUseCase`.
- [ ] Quitar acceso directo a `FlashcardRepository` y `DataStore` desde `QuoteRoute`.

## Fase 2: Convención de `UseCase` en `domain`

- [ ] Renombrar `DeckCreator` a `CreateDeckUseCase`.
- [ ] Renombrar `DeckFetcher` a `GetDecksUseCase`.
- [ ] Renombrar `DecksWithCardsProvider` a `GetDeckDetailUseCase` o `ObserveDeckWithCardsUseCase`.
- [ ] Renombrar `FlashcardCreator` a `CreateFlashcardUseCase`.
- [ ] Renombrar `FlashcardFetcher` a `GetStudySessionUseCase`.
- [ ] Renombrar `FlashcardFinder` a `GetFlashcardByIdUseCase`.
- [ ] Renombrar `FlashcardAndReviewFetcher` a `ObserveFlashcardsWithReviewUseCase`.
- [ ] Renombrar `FlashcardReviewUpdater` a `UpdateFlashcardReviewUseCase`.
- [ ] Renombrar `QuoteGenerator` a `GenerateQuoteUseCase`.
- [ ] Renombrar `QuoteLastFetcher` a `ObserveLatestQuoteUseCase` o `GetLatestQuoteUseCase`.
- [ ] Renombrar `BackupExecutor` a `RunBackupUseCase`.
- [ ] Actualizar Koin en `newModule.kt` después de cada rename.
- [ ] Ajustar imports y referencias antes de pasar al siguiente grupo de renombres.

## Fase 3: Estandarizar MVI

- [ ] Crear contrato base para `ViewModel` MVI si el equipo quiere reutilizar helpers.
- [ ] Definir snippet estándar por feature:
  - `private val _state = MutableStateFlow(UiState())`
  - `val state = _state.asStateFlow()`
  - `private val _effect = Channel<UiEffect>(Channel.BUFFERED)`
  - `val effect = _effect.receiveAsFlow()`
  - `fun onIntent(intent: UiIntent)`
- [ ] Convertir `NewDeckViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [ ] Crear `NewDeckUiEffect`.
- [ ] Crear `NewDeckUiIntent`.
- [ ] Renombrar `onAction` a `onIntent`.
- [ ] Mover la navegación de éxito de `NewDeckScreen` a un `UiEffect` emitido por `Channel`.
- [ ] Agregar `loading`, `error` y `success` explícitos al estado o efecto de `NewDeck`.
- [ ] Convertir `NewCardViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [ ] Crear `NewCardUiIntent`.
- [ ] Crear `NewCardUiEffect` para snackbar y navegación.
- [ ] Renombrar `onAction` a `onIntent`.
- [ ] Sustituir `SuccessConsumed` por emisión de efecto vía `Channel` o rediseño completo de estado, pero no mezclar ambos modelos.
- [ ] Convertir `StudyViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [ ] Crear `StudyUiIntent`.
- [ ] Renombrar `onProcess` a `onIntent`.
- [ ] Crear `StudyUiEffect` para fin de sesión y salida.
- [ ] Convertir `PairingViewModel` a `MutableStateFlow` + `asStateFlow()`.
- [ ] Crear `PairingUiIntent`.
- [ ] Crear `PairingUiEffect` para mensajes efímeros.
- [ ] Reemplazar métodos públicos como `createCode`, `joinWithCode`, `refreshDevices`, `revokeDevice` por intents.
- [ ] Normalizar nombres públicos de estado a `uiState` en todos los `ViewModel`.
- [ ] Normalizar nombre público de efectos a `effect` en todos los `ViewModel`.

## Fase 4: Mover lógica de negocio a `domain`

- [ ] Mover `ReviewGrade` a `domain` si forma parte de la lógica de negocio.
- [ ] Mover `SpacedRepetitionScheduler` a `domain`.
- [ ] Crear `ScheduleFlashcardReviewUseCase`.
- [ ] Hacer que `StudyViewModel` use el caso de uso en lugar del objeto en `app`.
- [ ] Extraer desde `QuoteRoute` toda la lógica de crear `CreateFlashcardInput`.
- [ ] Decidir si `CreateFlashcardInput` sigue siendo DTO de dominio o si conviene un comando más expresivo por caso de uso.

## Fase 5: Correcciones funcionales inmediatas

- [ ] Corregir `NewDeckUiState.isValid` para que la descripción realmente sea opcional.
- [ ] Evitar `onNavigateBack()` hasta confirmar creación exitosa del mazo.
- [ ] Añadir feedback visible si crear mazo falla.
- [ ] Reemplazar `zip` en `DeckDetailViewModel` por merge por `id`.
- [ ] Añadir prueba que falle si una lista tiene más elementos que la otra.

## Fase 6: Repositorios y SOLID

- [ ] Separar operaciones de generación IA fuera de `FlashcardRepository`.
- [ ] Crear una interfaz de lectura de flashcards.
- [ ] Crear una interfaz de escritura de flashcards.
- [ ] Crear una interfaz específica para sesión de estudio.
- [ ] Crear una interfaz específica para generación de contenido IA.
- [ ] Actualizar casos de uso para depender de la interfaz mínima necesaria.

## Fase 7: `detekt` como enforcement real

- [ ] Documentar que `./gradlew detekt` debe correr en cada PR.
- [ ] Crear tarea para vaciar primero baseline de `domain` y mantenerla vacía.
- [ ] Eliminar del baseline de `app` los issues de naming/filename más baratos.
- [ ] Renombrar `networkModule.kt`, `newModule.kt` y `repositoryModule.kt` para cumplir naming consistente.
- [ ] Eliminar del baseline de `data` los issues de `Filename` en `mappers.kt` y `providers.kt`.
- [ ] Resolver los `TooGenericExceptionCaught` más críticos.
- [ ] Resolver los `LongMethod` en pantallas grandes empezando por `NewCardScreen`.
- [ ] Resolver los `CyclomaticComplexMethod` de UI reusable (`HButton`, `HInput`) si siguen creciendo.
- [ ] Extraer `MagicNumber` de scheduler, workers y animaciones a constantes con nombre.
- [ ] Evaluar agregar una regla custom o convención revisable para forzar `*UseCase` en `domain`.

## Fase 8: Tests de regresión

- [ ] Añadir tests unitarios para `ScheduleFlashcardReviewUseCase`.
- [ ] Añadir tests unitarios para `CreateDeckUseCase`.
- [ ] Añadir tests unitarios para el flujo de pairing.
- [ ] Añadir tests unitarios para `SaveQuoteAsFlashcardUseCase`.
- [ ] Añadir tests del reducer o transición de estado en `NewDeck`.
- [ ] Añadir tests del reducer o transición de estado en `NewCard`.
- [ ] Añadir tests de emisión de `UiEffect` por `Channel`.
- [ ] Añadir tests para merge de tarjetas + reviews por `id`.
- [ ] Reemplazar los tests plantilla por pruebas reales o eliminarlos si no aportan valor.

## Primer sprint recomendado

- [ ] Corregir `NewDeckUiState.isValid`.
- [ ] Crear `NewDeckUiIntent`.
- [ ] Crear `NewDeckUiEffect`.
- [ ] Mover navegación de éxito/error de `NewDeck` a `Channel`.
- [ ] Reemplazar `zip` por merge por `id` en `DeckDetailViewModel`.
- [ ] Mover `SpacedRepetitionScheduler` a `domain`.
- [ ] Renombrar `DeckCreator` y `DeckFetcher` a `*UseCase`.
- [ ] Crear `GetDefaultDeckUseCase` y `SetDefaultDeckUseCase`.
- [ ] Quitar `DataStore` de `NewCardViewModel`.
- [ ] Limpiar 5 issues simples de `detekt` del baseline de `app`.

## Criterio de éxito

- Ningún `ViewModel` en `app` importa clases concretas de `data`, salvo la composición DI.
- Todos los casos de uso del dominio terminan en `UseCase`.
- Todas las features nuevas usan `UiState` + `UiIntent` + `UiEffect`.
- Todas las features nuevas exponen `state` por `StateFlow`.
- Todos los one-shot events salen por `Channel` y se consumen como `receiveAsFlow()`.
- Todos los `ViewModel` usan `onIntent(intent)` como entrada pública.
- `SpacedRepetitionScheduler` y reglas similares viven en `domain`.
- La baseline de `detekt` baja progresivamente en cada sprint.
- Existen tests reales sobre reglas y flujos críticos.
