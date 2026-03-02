# Code Review — Hello Android Project

> **Analizado por**: Android Developer Expert  
> **Fecha**: 2026-03-01  
> **Scope**: Módulos `app`, `domain`, `data` — código activo (`newfeatures`, `sync`, `data/remote`, `data/flashcard`, `data/deck`)

---

## Resumen de Severidad

| Severidad    | Cantidad |
|--------------|----------|
| 🔴 Crítico   | 3        |
| 🟠 Alto      | 5        |
| 🟡 Medio     | 6        |
| 🔵 Bajo      | 4        |
| **Total**    | **18**   |

---

## 🔴 Crítico

---

### [C-01] Llamada de red en `onCreate` sin ViewModel ni manejo de errores

**Archivo**: [`MainActivity.kt:21`](file:///Users/emm/AndroidStudioProjects/Hello/app/src/main/kotlin/com/emm/hello/MainActivity.kt)

```kotlin
// ❌ MAL
override fun onCreate(savedInstanceState: Bundle?) {
    lifecycleScope.launch {
        remote.export()  // llamada de red síncrona en el hilo principal del ciclo de vida
    }
}
```

**Problemas**:
- `lifecycleScope` se cancela si la Activity es destruida (rotación de pantalla reinicia la descarga)
- `export()` puede tardar varios segundos bloqueando la UI perceived startup
- Si falla, no hay forma de notificar al usuario ni reintentar
- `RemoteDataSource` inyectado directamente en la Activity viola el principio de separación de capas

**Solución**: Mover la lógica a un `ViewModel` con `viewModelScope`, o a un `WorkManager` como tarea de inicialización única. Usar el flag `firstInitializer` (ya existe) para hacerlo idempotente.

---

### [C-02] `populate()` es `public` en un repositorio — ruptura de encapsulamiento crítica

**Archivo**: [`DefaultBackupRepository.kt:128`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/remote/DefaultBackupRepository.kt)

```kotlin
// ❌ MAL
suspend fun populate() {  // debería ser private
    val syncResponse: FetchSyncResponse = backupService.fetchSync(androidId)
    // ... inserta directamente en la DB sin verificar duplicados
```

**Problemas**:
- `populate()` puede ser invocado desde cualquier lugar, corrompiendo la base de datos con duplicados si se llama múltiples veces
- No hay protección contra ejecución concurrente
- Viola el principio de encapsulamiento: la interfaz `BackupRepository` no declara este método, lo que significa que el contrato del dominio no protege este punto de entrada

**Solución**: Hacer `private suspend fun populate()`.

---

### [C-03] Uso de API interna de OkHttp (`okhttp3.internal`)

**Archivo**: [`DefaultBackupRepository.kt:22`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/remote/DefaultBackupRepository.kt)

```kotlin
import okhttp3.internal.toLongOrDefault  // ❌ API interna, no estable
```

**Problemas**:
- `okhttp3.internal.*` no es una API pública — puede cambiar o eliminarse sin aviso en cualquier actualización menor
- Ya causó `NoSuchMethodError` en proyectos reales con actualizaciones de OkHttp

**Solución**:
```kotlin
// ✅ BIEN
fun String.toLongOrDefault(default: Long) = toLongOrNull() ?: default
```

---

## 🟠 Alto

---

### [A-01] `SharedPreferences.Editor` compartido como `lazy val` — riesgo de race condition

**Archivo**: [`DataStore.kt:17`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/remote/DataStore.kt)

```kotlin
// ❌ MAL
private val editor: SharedPreferences.Editor by lazy { sharedPreferences.edit() }
```

**Problemas**:
- `SharedPreferences.Editor` **no es thread-safe**. Si dos coroutines llaman a `editor.putX().apply()` simultáneamente, los cambios pueden perderse
- `lazy` sin `LazyThreadSafetyMode.SYNCHRONIZED` puede inicializar el editor dos veces en entornos multihilo

**Solución**: Crear un nuevo `editor` por operación:
```kotlin
// ✅ BIEN
private fun edit(block: SharedPreferences.Editor.() -> Unit) {
    sharedPreferences.edit().apply(block).apply()
}
```

---

### [A-02] `NewCardViewModel` importa directamente `DataStore` de la capa `data`

**Archivo**: [`NewCardViewModel.kt:8`](file:///Users/emm/AndroidStudioProjects/Hello/app/src/main/kotlin/com/emm/hello/newfeatures/card/NewCardViewModel.kt)

```kotlin
import com.emm.data.remote.DataStore  // ❌ capa app consumiendo implementación de data
```

**Problemas**:
- El ViewModel de presentación depende directamente de una clase de implementación de `data`, no de una interfaz de `domain`
- Rompe el flujo de dependencias: `app → domain ← data`
- Hace imposible testear el ViewModel con un mock de `DataStore`

**Solución**: Crear una interfaz en `domain` (ej. `UserPreferences`) y que `DataStore` la implemente. El ViewModel depende de la interfaz.

---

### [A-03] `Log.e("TAG", ...)` con tag genérico hardcodeado en producción

**Archivos**: 
- [`RemoteDataSource.kt:54`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/deck/RemoteDataSource.kt)
- [`RemoteDataSource.kt:81`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/deck/RemoteDataSource.kt)

```kotlin
Log.e("TAG", "ZIPINPUT: ${e.message}")      // ❌
Log.e("TAG", "BufferedReader: ${e.message}")  // ❌
```

**Problemas**:
- Logs con tag `"TAG"` son imposibles de filtrar en Logcat en un proyecto real
- Los errores en `export()` son silenciados silenciosamente — el usuario no se entera si la importación falla
- Logs de debug llegan a producción (no hay guard de `BuildConfig.DEBUG`)

**Solución**: Usar `FirebaseCrashlytics.recordException(e)` (ya disponible en el proyecto) y un tag con el nombre de la clase: `private val TAG = RemoteDataSource::class.simpleName`.

---

### [A-04] `FlashcardSynchronizer` recibe `Context` pero solo lo usa para `WorkManager`

**Archivo**: [`FlashcardSynchronizer.kt:12`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/FlashcardSynchronizer.kt)

```kotlin
class FlashcardSynchronizer(
    db: HelloDb,
    private val remote: RemoteDataSource,
    private val context: Context,  // ❌ Android dependency en la capa data
)
```

**Problemas**:
- Inyectar `Context` en la capa `data` crea una dependencia de Android difícil de testear
- El `Context` solo se usa para llamar `WorkManager.getInstance(context)` — esto podría abstraerse
- Un sincronizador de datos no debería saber de `WorkManager` (mezcla de responsabilidades)

**Solución**: Separar la responsabilidad — el sincronizador solo sincroniza. El Worker llama al sincronizador. Eliminar `Context` del constructor.

---

### [A-05] `Prompt.buildPrompt()` genera un prompt crítico sin validación de entrada

**Archivo**: [`DefaultFlashcardRepository.kt:80`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt)

```kotlin
val prompt: String = Prompt.buildPrompt(word)  // 'word' viene sin sanitizar del usuario
val response: String = geminiService.process(prompt)
```

**Problemas**:
- La palabra ingresada por el usuario se inserta directamente en el prompt de Gemini sin sanitización
- Un usuario malintencionado puede hacer **prompt injection** (ej. `"ignore instructions and..."`), comprometiendo la calidad de las respuestas o revealing system prompt
- No hay límite en la longitud del `word` antes de enviar a la API

**Solución**: Validar y sanitizar `word` antes de usar en el prompt: longitud máxima, solo caracteres alfanuméricos/espacios, trim.

---

## 🟡 Medio

---

### [M-01] `GeminiService.cleanResponse()` usa manipulación de string frágil

**Archivo**: [`GeminiService.kt:14`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/GeminiService.kt)

```kotlin
private fun cleanResponse(response: String): String = response
    .removePrefix("```json")
    .removePrefix("```")   // ❌ orden de operaciones frágil
    .removeSuffix("```")
    .trim()
```

**Problemas**:
- Si Gemini devuelve ` ```json` (con espacio), el `removePrefix` no lo elimina
- Si el modelo devuelve ````kotlin` o cualquier otro lenguaje, falla silenciosamente
- No hay validación de que el resultado sea JSON válido antes de parsearlo

**Solución**: Usar regex: `"```[a-z]*\\n?(.*?)```"` con `DOTALL` o confiar en structured output de Gemini API.

---

### [M-02] Código comentado sin razón documentada en `Sync.kt`

**Archivo**: [`Sync.kt:20-24`](file:///Users/emm/AndroidStudioProjects/Hello/app/src/main/kotlin/com/emm/hello/sync/Sync.kt)

```kotlin
//            enqueueUniqueWork(
//                SYNC_WORK_NAME,
//                ExistingWorkPolicy.KEEP,
//                SyncWorker.startUpSyncWork(),
//            )
```

**Problemas**:
- Código muerto comentado sin explicar por qué fue desactivado ni cuándo debería reactivarse
- Genera confusión sobre el estado real del sistema de sincronización

**Solución**: Eliminar el código comentado. Si es necesario preservar la lógica, añadir un comentario explicativo o un TODO con ticket de referencia.

---

### [M-03] `FlashcardDetailViewModel` no maneja errores de `flashcardFinder.find()`

**Archivo**: [`FlashcardDetailViewModel.kt:22`](file:///Users/emm/AndroidStudioProjects/Hello/app/src/main/kotlin/com/emm/hello/newfeatures/card/FlashcardDetailViewModel.kt)

```kotlin
init {
    viewModelScope.launch {
        val flashcard = flashcardFinder.find(flashcardId)  // ❌ puede lanzar Exception
        _state.update { flashcard }
    }
}
```

`DefaultFlashcardRepository.fetchById()` lanza `throw Exception("Flashcard not found")`. Si falla, el ViewModel queda en estado `Flashcard.Empty` sin notificar al usuario.

**Solución**: Usar `runCatching` o un `sealed class` de UiState con estado de error.

---

### [M-04] `it -> it` shadowing en `DefaultFlashcardRepository.flashcardWithReview()`

**Archivo**: [`DefaultFlashcardRepository.kt:161`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt)

```kotlin
.map {
    it.map { it ->  // ❌ it shadowing: el lambda externo e interno ambos se llaman 'it'
        Flashcard(...)
    }
}
```

**Problemas**: El shadowing de `it` hace el código difícil de leer y puede causar bugs sutiles si se agrega lógica.

**Solución**: Usar nombres explícitos:
```kotlin
.map { entities ->
    entities.map { entity -> Flashcard(id = entity.id, ...) }
}
```

---

### [M-05] `Instant.now()` invocado múltiples veces con posible inconsistencia temporal

**Archivo**: [`DefaultBackupRepository.kt:136-137`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/remote/DefaultBackupRepository.kt)

```kotlin
createdAt = it.createdAt.toLongOrDefault(Instant.now().toEpochMilli()),
updatedAt = it.updatedAt?.toLongOrDefault(Instant.now().toEpochMilli()) ?: Instant.now().toEpochMilli(),
// ❌ Three separate Instant.now() calls - tiny window for time discrepancy
```

Aunque el impacto es mínimo, el patrón se repite para cada entidad en el `populate()`, lo que genera hasta 3 llamadas a `Instant.now()` por fila con timestamps potencialmente diferentes.

**Solución**: Capturar `val now = Instant.now().toEpochMilli()` una vez al inicio de `populate()`.

---

### [M-06] `DefaultFlashcardRepository.fetchById()` lanza `Exception` genérica

**Archivo**: [`DefaultFlashcardRepository.kt:113`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt)

```kotlin
val first = flashcardEntities.firstOrNull() ?: throw Exception("Flashcard not found")  // ❌ Exception genérica
```

**Problemas**: `Exception` genérica no puede ser capturada de forma selectiva por los llamadores — obliga a capturar todas las excepciones con `catch (e: Exception)`.

**Solución**: Crear una excepción del dominio:
```kotlin
// En domain/
class FlashcardNotFoundException(id: String) : Exception("Flashcard not found: $id")
```

---

## 🔵 Bajo

---

### [B-01] `BACKUP_SYNC_WORK_NAME` referenciado desde `WorkManagerSyncManager` sin importación directa

**Archivo**: [`WorkManagerSyncManager.kt:14`](file:///Users/emm/AndroidStudioProjects/Hello/app/src/main/kotlin/com/emm/hello/sync/WorkManagerSyncManager.kt)

La constante `BACKUP_SYNC_WORK_NAME` está declarada como `internal` en `Sync.kt`. Este acoplamiento implícito es difícil de seguir. Mejor centralizar las constantes de WorkManager en un objeto dedicado.

---

### [B-02] Nombres inconsistentes entre `generateFlashcard` y `generatedFlashcard`

**Archivo**: [`DefaultFlashcardRepository.kt:79,86`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt)

```kotlin
override suspend fun generateFlashcard(word: String): FlashcardGenerated   // ✓ verbo
override suspend fun generatedFlashcard(...): FlashcardGenerated            // ❌ participio pasado
```

Los nombres de funciones deberían ser verbos consistentes. `generatedFlashcard` suena como propiedad.

**Solución**: Renombrar a `generateFlashcardByCategory(...)`.

---

### [B-03] Código legacy activo en el DI (`casesModule`, `dataModule`, `homeModule`)

Los módulos `casesModule`, `dataModule`, y `homeModule` configuran dependencias de código marcado como `deprecated`, incluyendo la `AppDatabase` de Room. Esto significa que **dos bases de datos** están activas simultáneamente: Room (legacy) y SQLDelight (nueva), aumentando el footprint de memoria sin necesidad.

**Solución**: Planificar y ejecutar la migración final del legacy al nuevo sistema y eliminar los módulos obsoletos.

---

### [B-04] `ChuckerInterceptor` en producción

**Archivo**: [`providers.kt:27`](file:///Users/emm/AndroidStudioProjects/Hello/data/src/main/kotlin/com/emm/data/remote/providers.kt)

```kotlin
.addInterceptor(ChuckerInterceptor(context))  // ❌ siempre añadido, en debug Y release
```

Chucker tiene una versión `no-op` para release (ya declarada en `libs.versions.toml` como `library-no-op`), pero el código usa siempre la versión completa.

**Solución**: Usar `chucker:library` en `debugImplementation` y `chucker:library-no-op` en `releaseImplementation`.

---

## Plan de Acción Priorizado

| Prioridad | Issue | Esfuerzo | Impacto |
|-----------|-------|----------|---------|
| 1 | [C-01] `export()` en `onCreate` | Medio | 🔴 Estabilidad |
| 2 | [C-02] `populate()` público | Muy bajo | 🔴 Seguridad de datos |
| 3 | [C-03] `okhttp3.internal` | Muy bajo | 🔴 Compatibilidad |
| 4 | [A-01] Editor compartido en DataStore | Bajo | 🟠 Concurrencia |
| 5 | [A-02] `DataStore` en ViewModel | Medio | 🟠 Arquitectura |
| 6 | [A-03] `Log.e("TAG")` en producción | Bajo | 🟠 Observabilidad |
| 7 | [A-05] Prompt injection | Bajo | 🟠 Seguridad |
| 8 | [B-04] Chucker en producción | Muy bajo | 🔵 Performance |
| 9 | [M-01] `cleanResponse` frágil | Bajo | 🟡 Confiabilidad |
| 10 | [M-03] Sin error handling en FlashcardDetailVM | Bajo | 🟡 UX |
