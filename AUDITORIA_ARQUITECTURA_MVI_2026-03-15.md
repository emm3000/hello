# Auditoría de arquitectura y frontend

Fecha: 2026-03-15

## Alcance

Se auditó el proyecto contra estos criterios:

1. Clean Architecture alineada con la guía oficial de Android.
2. Frontend con MVI puro basado en este contrato:
   - `private val _state = MutableStateFlow(UiState())`
   - `val state = _state.asStateFlow()`
   - `private val _effect = Channel<UiEffect>(Channel.BUFFERED)`
   - `val effect = _effect.receiveAsFlow()`
   - `fun onIntent(intent: UiIntent)`
3. Aplicación de SOLID y buenas prácticas de legibilidad/mantenibilidad.

Fuentes oficiales usadas como referencia:

- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [UI events](https://developer.android.com/topic/architecture/ui-layer/events)

Puntos relevantes de la documentación oficial:

- Android recomienda arquitectura en capas con separación de responsabilidades, SSOT y flujo unidireccional de datos.
- Android recomienda que cada pantalla exponga `uiState` desde `ViewModel`, preferiblemente como `StateFlow`.
- La guía oficial separa claramente responsabilidades entre UI, `ViewModel`, dominio y datos.

## Resumen ejecutivo

El proyecto **no cumple hoy con MVI puro**. Lo que existe es una mezcla de **MVVM/UDF** con varias pantallas usando `mutableStateOf`, otras usando `StateFlow`, sin un contrato uniforme de `state`, sin un `Channel` dedicado para one-shot events y sin una entrada única por `onIntent(intent)`.

La base del proyecto sí tiene una intención razonable de capas (`app`, `domain`, `data`), pero **la Clean Architecture está perforada en la capa de presentación**: varios `ViewModel`, `Route` y `UiState` dependen directamente de tipos concretos del módulo `data`, y hay lógica de negocio ubicada dentro de `app`.

También hay problemas concretos de comportamiento:

- El flujo de crear mazo invalida el requerimiento "descripción opcional".
- La pantalla vuelve atrás antes de confirmar éxito/error al guardar.
- El detalle de mazo mezcla tarjetas y reviews usando `zip`, lo que puede truncar o desalinear datos.

Conclusión: la arquitectura actual es **recuperable**, pero todavía no puede considerarse una implementación sólida de Clean Architecture + MVI puro.

## Hallazgos

### 1. [Alta] La capa de presentación depende de implementaciones concretas del módulo `data`

Esto rompe el objetivo principal de Clean Architecture: que UI/presentación dependa de abstracciones de dominio, no de detalles de infraestructura.

Evidencia:

- `DashboardViewModel` usa `HelloDb` directamente para construir `SyncDebugUiState`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt:5-10`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt:23-38`
- `PairingViewModel` depende directamente de `HelloDb`, `LocalDeviceIdentityProvider` y `SupabaseSyncRemoteDataSource`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/pairing/PairingViewModel.kt:9-20`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/pairing/PairingViewModel.kt:123-164`
- `NewCardViewModel` depende de `DataStore` del módulo `data`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt:8`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt:15-35`
- `PairingUiState` expone `LinkedDevice`, que pertenece a `data`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/pairing/PairingUiState.kt:3-12`
- `QuoteRoute` inyecta y usa repositorios directamente desde el composable, incluyendo `DataStore`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/QuoteRoute.kt:11-16`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/QuoteRoute.kt:30-64`

Impacto:

- Reduce testabilidad de presentación.
- Aumenta acoplamiento entre UI e infraestructura.
- Hace más costoso reemplazar persistencia, sync o transporte.
- Debilita DIP y SRP.

Veredicto:

- `Clean Architecture`: **No cumple** en la capa de presentación.

### 2. [Alta] El frontend no implementa el contrato MVI definido para el proyecto

Si el estándar esperado es el acordado para este proyecto, cada feature debería exponer exactamente:

- `UiState`
- `UiIntent`
- `UiEffect`
- `private val _state = MutableStateFlow(...)`
- `val state = _state.asStateFlow()`
- `private val _effect = Channel<UiEffect>(Channel.BUFFERED)`
- `val effect = _effect.receiveAsFlow()`
- `fun onIntent(intent: UiIntent)`

Lo encontrado es una mezcla:

- `DashboardViewModel` usa `StateFlow`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/DashboardViewModel.kt:40-53`
- `DeckDetailViewModel` usa `StateFlow`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailViewModel.kt:23-39`
- `FlashcardDetailViewModel` usa `MutableStateFlow`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt:18-25`
- `NewCardViewModel`, `NewDeckViewModel`, `StudyViewModel` y `PairingViewModel` usan `mutableStateOf`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt:21-22`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckViewModel.kt:14-15`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt:20-21`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/pairing/PairingViewModel.kt:23-24`

Además:

- no existe `Channel` de efectos por pantalla,
- no existe `receiveAsFlow()` para efectos,
- no existe una entrada uniforme `onIntent(intent)`,
- se mezclan nombres como `onAction`, `onProcess`, `createCode`, `joinWithCode`, `refreshDevices`.

Los eventos transitorios se resuelven de forma ad hoc:

- `NewCardScreen` observa `state.isSuccess`, muestra `Snackbar` y luego manda `SuccessConsumed`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardScreen.kt:136-148`
- `NewDeckScreen` navega hacia atrás directamente desde la UI al presionar guardar, sin esperar resultado.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt:54-63`
- `PairingViewModel` mezcla estado persistente con mensajes efímeros (`success`, `error`) en el mismo `UiState`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/pairing/PairingUiState.kt:5-14`
- `StudyViewModel` tampoco usa `onIntent`, sino `onProcess`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt:46-57`

Impacto:

- No hay un patrón uniforme por feature.
- Los one-shot events no tienen una canalización consistente.
- Se vuelve más difícil razonar, testear y escalar el frontend.

Veredicto:

- `MVI puro`: **No cumple**.

Nota:

- La guía oficial de Android favorece `uiState` y advierte contra ciertos patrones de "one-off events" desde `ViewModel`. Aun así, dado que la decisión del proyecto es usar MVI clásico con `Channel` para one-shot events, la auditoría evalúa contra ese contrato explícito. Ahora mismo el proyecto no sigue ese contrato.

### 3. [Alta] Hay lógica de negocio y orquestación de datos ubicada en `app`

La lógica de negocio reutilizable debería residir en dominio o, como mínimo, detrás de casos de uso. Aquí hay reglas importantes viviendo en `app`.

Evidencia:

- El algoritmo de repetición espaciada está en `app`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/study/SpacedRepetitionScheduler.kt:1-60`
- `StudyViewModel` usa ese scheduler directamente.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/study/StudyViewModel.kt:46-57`
- `QuoteRoute` crea tarjetas directamente desde un composable usando `FlashcardRepository` y `CreateFlashcardInput`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/dashboard/QuoteRoute.kt:39-64`

Impacto:

- Las reglas de negocio no son reutilizables fuera de la pantalla.
- Se mezcla lógica de aplicación con UI/navigation.
- Se dificulta testear la lógica sin Compose o sin framework Android.

Veredicto:

- `Separation of concerns`: **No cumple** en estos flujos.

### 4. [Media] El flujo de creación de mazo tiene dos defectos funcionales

Evidencia:

- La UI muestra la descripción como opcional.
  - `app/src/main/res/values/strings.xml:52-54`
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt:91-107`
- Pero `isValid` exige que `description` no esté vacía.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckUiState.kt:3-9`
- Además la pantalla hace `onNavigateBack()` inmediatamente al pulsar guardar.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckScreen.kt:54-60`
- El `ViewModel` no expone ni `loading`, ni `error`, ni `success`.
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/NewDeckViewModel.kt:12-31`

Impacto:

- El usuario no puede guardar un mazo solo con nombre, aunque el copy diga lo contrario.
- La pantalla se cierra antes de saber si la operación terminó bien o falló.
- No existe feedback ante error.

Veredicto:

- `UX + mantenibilidad`: **No cumple**.

### 5. [Media] `DeckDetailViewModel` mezcla listas con `zip`, lo que puede truncar tarjetas o asignar reviews incorrectos

Evidencia:

- La combinación se hace por posición:
  - `app/src/main/kotlin/com/emm/hello/newfeatures/deck/DeckDetailViewModel.kt:23-33`
- El merge concreto usa:
  - `deck.cards.zip(sessionCards) { deckCard, sessionCard -> deckCard.copy(review = sessionCard.review) }`

Problema:

- `zip` trunca al tamaño mínimo.
- Si cambia el orden entre ambos flujos o una lista trae más/menos elementos, se pierden tarjetas.
- La unión correcta debería ser por `id`, no por índice.

Impacto:

- Riesgo de mostrar estado de repaso equivocado.
- Riesgo de ocultar tarjetas válidas.

Veredicto:

- `Corrección funcional`: **No cumple**.

### 6. [Media] `FlashcardRepository` concentra demasiadas responsabilidades

El repositorio mezcla responsabilidades de persistencia, consulta, sesión de estudio y generación con IA.

Evidencia:

- Interfaz:
  - `domain/src/main/kotlin/com/emm/domain/flashcard/FlashcardRepository.kt:5-24`
- Implementación:
  - `data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt:31-260`

En una sola abstracción conviven:

- creación de flashcards,
- persistencia de ejemplos,
- generación con Gemini,
- lecturas por deck,
- búsqueda por id,
- sesión del día,
- tarjetas con review.

Impacto:

- Viola SRP e Interface Segregation.
- Los casos de uso de dominio terminan siendo wrappers muy delgados.
- El módulo de dominio expresa operaciones de infraestructura y de IA en una misma interfaz.

Veredicto:

- `SOLID`: **Cumplimiento parcial / débil**.

### 7. [Media] La capa `domain` no sigue una convención consistente de casos de uso con sufijo `UseCase`

Si el equipo quiere alinearse con la convención promovida en la documentación y samples de Android, los casos de uso deberían nombrarse explícitamente como `...UseCase`.

Hoy aparecen clases de dominio que conceptualmente son casos de uso, pero con nombres heterogéneos:

- `DeckCreator`
  - `domain/src/main/kotlin/com/emm/domain/deck/DeckCreator.kt:3-7`
- `DeckFetcher`
  - `domain/src/main/kotlin/com/emm/domain/deck/DeckFetcher.kt:5-9`
- `FlashcardCreator`
  - `domain/src/main/kotlin/com/emm/domain/flashcard/FlashcardCreator.kt:3-39`
- `FlashcardFetcher`
  - `domain/src/main/kotlin/com/emm/domain/flashcard/FlashcardFetcher.kt:3-7`
- `FlashcardFinder`
  - `domain/src/main/kotlin/com/emm/domain/flashcard/FlashcardFinder.kt:3-7`
- `FlashcardAndReviewFetcher`
  - `domain/src/main/kotlin/com/emm/domain/flashcard/FlashcardAndReviewFetcher.kt:5-9`
- `QuoteGenerator`
  - `domain/src/main/kotlin/com/emm/domain/quote/QuoteGenerator.kt:3-7`
- `QuoteLastFetcher`
  - `domain/src/main/kotlin/com/emm/domain/quote/QuoteLastFetcher.kt:5-9`
- `BackupExecutor`
  - `domain/src/main/kotlin/com/emm/domain/backup/BackupExecutor.kt:3-7`

Problema:

- El módulo `domain` mezcla entidades, providers, servicios de dominio y casos de uso sin una convención visual fuerte.
- La intención de cada clase no se identifica tan rápido como con un sufijo estándar.
- La DI también refleja esa ambigüedad:
  - `app/src/main/kotlin/com/emm/hello/di/newModule.kt:94-106`

Impacto:

- Baja discoverability del dominio.
- Aumenta la fricción al navegar y revisar código.
- Complica estandarizar arquitectura entre features.

Veredicto:

- `Legibilidad de dominio`: **No cumple** con la convención pedida.

### 8. [Media] `detekt` existe y pasa, pero hoy actúa más como cerco con baseline que como mecanismo real de saneamiento continuo

Evidencia:

- Se configuró `maxIssues: 0`, lo cual es correcto como objetivo:
  - `config/detekt/detekt.yml:1-3`
- Todos los subproyectos usan baseline por módulo:
  - `build.gradle.kts:14-22`
- `domain` tiene baseline vacía:
  - `domain/detekt-baseline.xml`
- `app` y `data` tienen baselines con una cantidad importante de issues históricos:
  - `app/detekt-baseline.xml`
  - `data/detekt-baseline.xml`
- La ejecución actual pasa:
  - `./gradlew detekt` ✅

Señales concretas de deuda absorbida por baseline:

- `LongMethod`, `CyclomaticComplexMethod`, `MagicNumber`, `TooGenericExceptionCaught`.
- Issues de nombre/archivo:
  - `Filename:networkModule.kt`
  - `Filename:newModule.kt`
  - `Filename:repositoryModule.kt`
  - `Filename:mappers.kt`
  - `Filename:providers.kt`
- No hay una regla visible en `detekt.yml` que haga cumplir convención de clases de caso de uso con sufijo `UseCase`.
  - `config/detekt/detekt.yml:41-53`

Problema:

- El pipeline da señal verde, pero no representa un estado limpio del código.
- Varias reglas útiles ya detectaron problemas importantes y quedaron normalizadas por baseline.
- La convención de naming arquitectónico no está automatizada.

Impacto:

- Se reduce la capacidad de `detekt` para empujar mejora incremental real.
- La deuda se estabiliza en lugar de reducirse.
- La arquitectura depende de disciplina manual, no de enforcement automático.

Veredicto:

- `Calidad estática`: **Cumplimiento parcial / débil**.

### 9. [Media] La cobertura de pruebas no protege la arquitectura actual

Evidencia:

- Solo existen tests plantilla:
  - `app/src/test/java/com/emm/hello/ExampleUnitTest.kt:11-15`
  - `app/src/androidTest/java/com/emm/hello/ExampleInstrumentedTest.kt:17-23`
- `:domain:test` no tiene pruebas reales.

Impacto:

- No hay red de seguridad para refactor arquitectónico.
- Reglas críticas como scheduler SM-2, reducers de estado y casos de uso quedan sin contrato verificable.

Veredicto:

- `Mantenibilidad`: **No cumple**.

## Matriz de cumplimiento

| Criterio | Estado | Observación |
|---|---|---|
| Separación por módulos `app/domain/data` | Parcial | La intención existe, pero `app` filtra tipos concretos de `data`. |
| Dominio independiente de Android | Sí | En general el módulo `domain` se mantiene limpio. |
| Presentación desacoplada de datos | No | Hay dependencias directas a `HelloDb`, `DataStore`, `SupabaseSyncRemoteDataSource`, `LinkedDevice`. |
| UDF / `uiState` consistente | Parcial | Algunas pantallas usan `StateFlow`, otras `mutableStateOf`. |
| MVI puro con `StateFlow` + `Channel` + `onIntent` | No | No hay contrato uniforme de `state`, `effect` ni `onIntent`. |
| Convención de casos de uso `*UseCase` | No | Los casos de uso existen, pero con nombres heterogéneos como `Fetcher`, `Creator`, `Finder`, `Executor`. |
| SOLID | Parcial | Hay buenas intenciones, pero repositorios grandes y responsabilidades mezcladas. |
| Legibilidad y mantenibilidad | Parcial | Estructura general legible, pero con inconsistencias arquitectónicas relevantes. |
| `detekt` como enforcement real | Parcial | La configuración existe, pero `app` y `data` dependen de baseline amplia. |
| Testabilidad | Baja | No hay pruebas útiles sobre reglas o capas. |

## Lo que sí está bien encaminado

- Existe separación física por módulos Gradle.
- El módulo `domain` no arrastra dependencias Android.
- Se usan `Flow` y `collectAsStateWithLifecycle()` en varias pantallas.
- Hay `UiState` por feature en varios casos.
- Hay una intención clara de encapsular acceso a datos a través de repositorios y casos de uso.

Esto significa que la base no está perdida; el problema principal es la disciplina de fronteras entre capas y la falta de una convención frontend única.

## Recomendación de refactor priorizada

### Fase 1

- Sacar de `app` toda dependencia directa a `HelloDb`, `DataStore`, `SupabaseSyncRemoteDataSource` y DTO/modelos de `data`.
- Crear casos de uso específicos para:
  - pairing,
  - lectura de sync debug,
  - guardar quote como flashcard,
  - leer/escribir deck por defecto.
- Mover `SpacedRepetitionScheduler` al módulo `domain`.

### Fase 2

- Definir un contrato MVI único por feature:
  - `UiState`
  - `UiIntent`
  - `UiEffect`
  - `onIntent(...)`
  - `_state: MutableStateFlow`
  - `_effect: Channel`
- Exponer estado de pantallas siempre como `StateFlow` vía `asStateFlow()`.
- Exponer efectos como `receiveAsFlow()`.
- Eliminar `mutableStateOf` de `ViewModel` salvo una justificación muy puntual y documentada.
- Renombrar casos de uso del dominio a convención `*UseCase`.

### Fase 3

- Dividir `FlashcardRepository` en abstracciones más pequeñas, por ejemplo:
  - `FlashcardWriteRepository`
  - `FlashcardQueryRepository`
  - `FlashcardGenerationRepository`
  - `FlashcardSessionRepository`
- Evitar que el dominio exprese detalles de IA dentro de la misma interfaz de persistencia.

### Fase 4

- Añadir tests unitarios para:
  - scheduler SM-2,
  - reducers/intents por pantalla,
  - casos de uso de pairing,
  - validación del flujo de `NewDeck`,
  - merge de tarjetas + reviews por `id`.

### Fase 5

- Reducir progresivamente `detekt-baseline.xml` en `app` y `data`.
- Añadir enforcement de naming arquitectónico para casos de uso.
- Convertir los hallazgos hoy baselined de mayor valor en trabajo real:
  - `LongMethod`
  - `CyclomaticComplexMethod`
  - `TooGenericExceptionCaught`
  - `MagicNumber`
  - `Filename`

## Validación realizada

Revisión estática:

- estructura de módulos,
- dependencias entre capas,
- `ViewModel`, `Route`, `Screen`, `UiState`, repositorios y casos de uso clave.

Validación por build:

- `./gradlew :app:testDebugUnitTest` ✅
- `./gradlew :domain:build` ✅
- `./gradlew detekt` ✅

Observación:

- El build pasa, pero eso no contradice los problemas de arquitectura; solo confirma que el proyecto compila y que los tests actuales son insuficientes para detectarlos.

## Dictamen final

Si el estándar exigido es:

- Clean Architecture alineada con Android,
- frontend con MVI puro,
- SOLID fuerte,
- alta mantenibilidad,

entonces el estado actual del código es **insuficiente**.

Mi evaluación global es:

- **Arquitectura general:** 5/10
- **Cumplimiento de Clean Architecture:** 4/10
- **Cumplimiento de MVI puro:** 2/10
- **Convención de casos de uso en `domain`:** 3/10
- **SOLID y mantenibilidad:** 4/10
- **Madurez de `detekt`:** 5/10
- **Base para refactor:** 7/10

El proyecto tiene una base modular aceptable, pero necesita un refactor arquitectónico real para alcanzar el estándar que pediste.
