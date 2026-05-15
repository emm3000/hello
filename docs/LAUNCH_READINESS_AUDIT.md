# Launch Readiness Audit

| Field | Value |
|---|---|
| Status | Active |
| Role | Auditoría completa pre-lanzamiento + plan de fases atómicas |
| Source of Truth | Yes (mientras no se cierre todo) |
| Read this when | Vas a trabajar en cualquier tarea de hardening pre-lanzamiento |
| Última verificación contra código | 2026-05-14 |
| Progreso Sprint 1 | 6/8 completados (T1, T2, T4, T5, T6, T8) · T3 descartado · pendiente T7 |

## TL;DR

**No hay que rehacer el proyecto.** La base es sólida y honesta con su contrato local-first. Hay ~10 fixes concretos (mayoría configuración + prompts + un cambio de modelo) que bloquean Play Store o degradan UX en producción. Estimo **2-3 días de trabajo efectivo**, organizados en 3 sprints.

| Capa | Nota | Estado |
|---|---|---|
| Domain | 7.5/10 | Sólido. Value objects firmes, policies separadas. Grietas en `Flashcard` anémica + scheduler SM-2 sin documentar. |
| Data | 8/10 | Schema limpio, soft-delete consistente, backup round-trip testeado. 2 hotfixes reales. |
| AI / Prompts | 6/10 | Arquitectura buena, pero modelo demasiado liviano + quality checks auto-sellados + error handling frágil. Capa más débil. |
| Arquitectura | 7/10 | Boundaries respetados, DI coherente, MVI sin overengineering. Falta observabilidad y release hardening. |

---

## 1. Análisis por capa

### 1.1 Domain (`:domain`)

**Strengths**
- Value objects con normalización en constructor: `Expression`, `IntendedMeaningEs`, `DefinitionEn`, `FlashcardId`, `DeckId` (`@JvmInline`, trim, whitespace collapse).
- `FlashcardReview` con invariantes reales (`easeFactor ≥ 1.3`, no-negativas).
- Policies separadas y testables: `CoreFieldsPolicy`, `CardsPolicy`, `QualityChecksPolicy`, `TypeRequirementsPolicy` para `GeneratedLearningNote`; `ContextSentence`, `Disambiguation`, `InputTypeRules`, `WordCount` para `FlashcardGenerationInput`.
- Use cases cohesivos (`CreateFlashcardUseCase`, `ScheduleFlashcardReviewUseCase`) sin orchestration innecesaria.
- Domain JVM-only respetado, repositorios como interfaces.

**Grietas**
- 🟡 `Flashcard` es anémica: `word/meaning/translation` son `String` crudos sin invariantes (`domain/src/main/kotlin/com/emm/domain/flashcard/Flashcard.kt`). Ya existen los value objects, no se usan en el agregado principal.
- 🟡 `SpacedRepetitionScheduler` usa SM-2 simplificado con fórmula custom no documentada (`domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt`). El delta de ease (`0.1 - qualityDistance * (0.08 + qualityDistance * 0.02)`) no tiene paper de referencia. Para una app de estudio, ese es el corazón.
- 🟢 `UpdateFlashcardUseCase` y `SoftDeleteFlashcardUseCase` son forwarders triviales que envuelven el repo. No agregan lógica.
- 🟢 Naming inconsistente entre policies: `FlashcardGeneration*Policy` vs `GeneratedLearningNote*Policy`. No bloquea, agrega fricción cognitiva.

### 1.2 Data (`:data`)

**Strengths**
- Schema normalizado con UUIDs naturales; FKs con `ON DELETE CASCADE`; soft-delete (`deletedAt`) consistente en `Deck`, `Flashcard`, `FlashcardExample`, `Tag`.
- Índices en `deletedAt` y composites (`deckId + deletedAt`); índice DESC en `createdAt`.
- Transacciones explícitas en operaciones multi-tabla; tests de soft-delete visibility (`SoftDeleteVisibilityQueryTest`).
- Backup round-trip validado (`ExportImportIntegrationTest`); import idempotente dentro de transacción; JSON con `ignoreUnknownKeys = true` para forward-compat.
- `LocalDeviceIdentity` thread-safe (`INSERT OR IGNORE`, singletonId fijo).

**Grietas**
- 🔴 No existe carpeta `migrations/` en `data/src/main/sqldelight/`. Si v1.0 sale y v1.1 cambia schema, el upgrade rompe. Hay que crear baseline ANTES de tener usuarios.
- 🔴 `DeckTag` no propaga soft-delete: `Tag` tiene `deletedAt` pero `DeckTag` no. El `ON DELETE CASCADE` solo dispara si el tag se borra duro. El export incluye `DeckTag` huérfanos.
- 🟡 No hay validación de `schemaVersion` en import; un backup de v2 importado en app v1 puede truncar la DB.
- 🟢 Catálogos estáticos (`StaticCategories`, `CommunicativeIntent`) sin i18n.

### 1.3 AI / Prompts (`data/.../flashcard/`)

**Strengths**
- Role + principios explícitos en el prompt principal (bilingual EN-learning assistant for native Spanish speakers).
- Decision policy clara: priorización por nivel, frecuencia > reusability.
- Schema JSON estructurado con 7 quality checks + discriminación por nota_type.
- Regeneraciones parciales (Field/Cloze/Example/StudyCard) reutilizan la nota existente, no rehacen el trabajo.
- Parser tipado con DTOs separados (`data/.../flashcard/iadto/`).

**Grietas críticas**
- 🔴 Modelo `gemini-2.5-flash-lite` es el más débil de la familia 2.5. Esperar ~30% de notas mediocres (traducciones literales, ejemplos textbook, IPA ocasionalmente errado). Cambiar a `gemini-2.5-flash`.
- 🔴 Sin `responseSchema` en `generationConfig` (`app/.../di/RepositoryModule.kt:30-31`, solo `responseMimeType = "application/json"`). El modelo puede devolver enums con casing inconsistente; el parser explota con `IllegalArgumentException` genérico.
- 🔴 Error handling frágil en `GeminiService`: si Gemini retorna null/error, devuelve `""`, el parser lanza excepción sin causa original. Sin retry, sin backoff, sin timeout explícito, sin log del raw response.
- 🟡 Quality checks son **auto-sellados**: el prompt le pide al modelo que rellene `passed: true/false` para sus propios outputs. `QualityChecksPolicy` solo lee la decisión del modelo. Es burocracia útil pero no es validación.
- 🟡 Lenguaje mezclado (es/en) en el mismo prompt. Inputs en español interpolados en system prompt en inglés confunden al modelo.
- 🟢 Sin few-shot examples en prompts de regeneración. `gemini-2.5-flash-lite` rinde mejor con 1-2 ejemplos.

### 1.4 Arquitectura y wiring

**Strengths**
- Module boundaries respetados (`app -> data`, `app -> domain`, `data -> domain`). Domain JVM-only verificado.
- Koin coherente: cada `Repository` del domain tiene impl bindeada en data; cada VM registrado (incluido `AppStartupViewModel` en `NewModule.kt:167`).
- Base MVI (`MviViewModel<S, I, E>`) hace lo justo. No middleware, no saga.
- Navigation 3 bien integrada: `rememberNavBackStack`, decorators correctos, transiciones consistentes.
- Tests del domain bien cubiertos (~30 archivos).

**Grietas**
- 🔴 `App.kt` solo arranca Koin + `AppStartupCoordinator.start()`. Firebase Crashlytics y Analytics están en gradle pero **nunca se inicializan**. Sin esto, lanzar = volar ciego.
- 🔴 `app/proguard-rules.pro` y `data/proguard-rules.pro` están vacíos (solo comentarios). En release con minify, Firebase AI / kotlinx-serialization / SQLDelight pueden romper en runtime.
- 🟡 No hay timeout en startup: si `LocalIdentityInitializer.ensureReady()` cuelga, loading infinito.
- 🟡 `POST_NOTIFICATIONS` permission en `AndroidManifest.xml` sin notificaciones implementadas. Play Console preguntará.
- 🟡 ViewModels sin tests unitarios. Solo domain está bien cubierto.
- 🟢 Sin privacy policy URL en `AndroidManifest`. Requerido por Play Store (Data Safety section).

---

## 2. Plan en fases atómicas

Cada tarea es independiente, tiene **archivo afectado**, **criterio de aceptación**, y **estimación**.
Marcá como `[x]` al completar. Las dependencias entre tareas están explícitas.

### Sprint 1 — Bloqueantes Play Store (objetivo: 1-2 días)

#### S1-T1: Inicializar Crashlytics y Analytics en App
- **Archivo:** `app/src/main/kotlin/com/emm/hello/App.kt`
- **Por qué:** sin esto no hay señales del campo en producción.
- **Qué hacer:** en `onCreate()` antes de `startKoin`, llamar `FirebaseApp.initializeApp(this)`, habilitar `FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true`, instanciar `FirebaseAnalytics.getInstance(this)`.
- **Criterio:** abrir la app debug, forzar crash, verlo en Crashlytics console dentro de 5 min. Verificar evento `first_open` en Analytics DebugView.
- **Estimación:** 30 min.
- **Estado:** [x] — `App.kt` inicializa `FirebaseApp`, habilita Crashlytics y instancia Analytics antes de Koin (commit `8e8e5dd`). Validación manual en device pendiente.

#### S1-T2: Reglas R8/ProGuard para Firebase y serialization
- **Archivos:** `app/proguard-rules.pro`, `data/proguard-rules.pro`
- **Por qué:** un build release con minify puede romper deserialización de respuesta de Gemini sin error en compile.
- **Qué hacer:** agregar keep rules para `com.google.firebase.**`, `kotlinx.serialization.**`, todas las `@Serializable data class` del proyecto (DTOs en `data/.../flashcard/iadto/`, `BackupEnvelope`), SQLDelight runtime classes.
- **Criterio:** `./gradlew :app:assembleRelease` y correr el APK en device real. Crear una flashcard via Gemini sin error de deserialización. Export/import backup sin errores.
- **Estimación:** 1-2 h (incluye iteración cuando algo se rompe en runtime).
- **Estado:** [x] — keep rules agregadas en `app/proguard-rules.pro` (Crashlytics deobfuscation, kotlinx.serialization oficial, Firebase defensivo, `@Serializable` del proyecto) y `data/consumer-rules.pro` (DTOs de `:data`, `HelloDb` de SQLDelight). `data/proguard-rules.pro` queda intacto porque `:data` no minifica. `assembleRelease` verificado con R8 (commit `8e8e5dd`). Validación funcional en device pendiente.
- **Depende de:** S1-T1 (para ver crashes en Crashlytics si algo falla).

#### S1-T3: Cambiar modelo Gemini a `gemini-2.5-flash` ~~(descartado por decisión del usuario)~~
- **Archivo:** `app/src/main/kotlin/com/emm/hello/di/RepositoryModule.kt:29`
- **Por qué:** flash-lite produce ~30% de notas mediocres en lingüística. Costo extra estimado: ~$40/día a 10k users × 5 cards/día.
- **Qué hacer:** cambiar `modelName = "gemini-2.5-flash-lite"` a `"gemini-2.5-flash"`.
- **Criterio:** generar 10 flashcards de palabras variadas (high-frequency, phrasal verbs, idioms, latinismos). Comparar manualmente con outputs previos. Naturalidad de ejemplos y IPA deberían mejorar.
- **Estimación:** 15 min + 30 min de validación manual.
- **Estado:** [~] — descartado: usuario decidió mantener `gemini-2.5-flash-lite` por ahora. Re-evaluar tras feedback de beta si calidad lingüística degrada el producto.

#### S1-T4: Agregar `responseSchema` explícito en `generationConfig`
- **Archivos:** `data/src/main/kotlin/com/emm/data/flashcard/LearningNoteResponseSchema.kt` (nuevo), `data/src/main/kotlin/com/emm/data/flashcard/GeminiService.kt`, `data/src/main/kotlin/com/emm/data/flashcard/DefaultFlashcardRepository.kt`, `app/src/main/kotlin/com/emm/hello/di/RepositoryModule.kt`.
- **Por qué:** sin schema, el modelo puede devolver enums con casing inconsistente y el parser explota con error genérico.
- **Qué hacer:** declarar `responseSchema` con la estructura de `GeneratedLearningNoteResponseDto` (wrapper `{success, data, error}`) incluyendo `Schema.enumeration(...)` para `note_type`, `part_of_speech`, `register`, `level_band`, `domain`, `card_type`, `evaluation_mode`, `quality_checks.code`. Scope: sólo afecta la generación principal; las regeneraciones parciales mantienen el modelo genérico.
- **Criterio:** generar 20 flashcards; el parser nunca debería lanzar `IllegalArgumentException` por casing/enum. Validación funcional pendiente en device (requiere build release + lote de generaciones).
- **Estimación:** 2-3 h (definir schema completo es tedioso pero mecánico).
- **Estado:** [x] — schema completo en `LearningNoteResponseSchema` (note + study cards + quality checks + error envelope) con `optionalProperties` alineados a los defaults del DTO. `GeminiService` ahora expone `processLearningNote(prompt)` que usa un `GenerativeModel` dedicado con `responseSchema`; las regeneraciones parciales siguen usando `process(prompt)` con el modelo sin schema (preserva shapes distintos). `DefaultFlashcardRepository.generateLearningNote` re-ruteado al método nuevo. `:data` tests + detekt en verde. Validación manual en device queda para S2-T6.

#### S1-T5: Migrations baseline en SQLDelight
- **Archivos:** `data/build.gradle.kts`, `data/src/main/sqldelight/databases/1.db` (generado), `ARCHITECTURE.md`.
- **Por qué:** si v1.0 sale y v1.1 cambia schema, el upgrade rompe. Es gratis hacerlo ahora.
- **Qué hacer:** configurar `schemaOutputDirectory` + `verifyMigrations = true` en el bloque sqldelight. Generar baseline con `./gradlew :data:generateDebugHelloDbSchema`. Documentar política en `ARCHITECTURE.md`.
- **Criterio:** `./gradlew :data:verifySqlDelightMigration` pasa. Documentar en `ARCHITECTURE.md` la política: "cada cambio de schema requiere `N.sqm` correspondiente".
- **Estimación:** 1 h.
- **Estado:** [x] — `schemaOutputDirectory.set(file("src/main/sqldelight/databases"))` + `verifyMigrations.set(true)` agregados al bloque `sqldelight` de `data/build.gradle.kts`. Baseline `1.db` generado y commiteado en `data/src/main/sqldelight/databases/`. `verifySqlDelightMigration` pasa. Política de migraciones documentada en `ARCHITECTURE.md`. Decisión: NO se creó `1.sqm` vacío (bumpearía el schema a v2 sin cambios reales); el `.db` baseline es suficiente para que `verifyMigrations` detecte futuras divergencias.

#### S1-T6: Soft-delete cascada en `DeckTag`
- **Archivos:** `data/src/main/sqldelight/com/emm/data/Export.sq`, `data/src/test/kotlin/com/emm/data/export/ExportImportIntegrationTest.kt`.
- **Por qué:** tags soft-deleted dejan `DeckTag` huérfanos en el export.
- **Qué hacer:** filtrar `allDeckTagsPaged` con JOIN a `Tag` y `Deck` exigiendo `deletedAt IS NULL` en ambos. Decisión: NO se agrega `deletedAt` a `DeckTag` (evita migration y mantiene `DeckTag` como tabla de unión pura; el filtrado en el query es suficiente porque el importer reescribe `DeckTag` desde el envelope).
- **Criterio:** dos tests nuevos en `ExportImportIntegrationTest`: (1) tag soft-deleted con DeckTag activo → import en BD limpia deja solo 1 DeckTag (el del tag activo). (2) deck soft-deleted con DeckTag → import en BD limpia deja 0 DeckTag.
- **Estimación:** 2 h.
- **Estado:** [x] — `allDeckTagsPaged` ahora hace `JOIN Tag JOIN Deck` con doble filtro `deletedAt IS NULL`. Tests `soft-deleted tag does not leak DeckTag rows into export` y `soft-deleted deck does not leak DeckTag rows into export` agregados y verdes. Sin cambios de schema → no requiere `N.sqm`.

#### S1-T7: Privacy policy + Data Safety
- **Archivos:** publicar política externa (URL), agregar `meta-data` o referencia en `app/src/main/AndroidManifest.xml`, completar Data Safety form en Play Console.
- **Por qué:** Play Store rechaza apps que envían input del usuario a un LLM sin declararlo.
- **Qué hacer:** redactar política mínima que cubra: deviceId local, input del usuario enviado a Firebase AI / Gemini, Crashlytics, Analytics. Publicarla (GitHub Pages, Notion público, etc.). En Play Console, marcar: "data collected: app activity, app info, device IDs", "shared with third parties: Google Firebase AI".
- **Criterio:** Play Console acepta el Data Safety form en pre-validación.
- **Estimación:** 2 h (redacción + publicación + form).
- **Estado:** [ ]

#### S1-T8: Limpiar permisos no usados
- **Archivo:** `app/src/main/AndroidManifest.xml`
- **Qué hacer:** verificar `POST_NOTIFICATIONS` — si no hay notificaciones implementadas, removerlo. Verificar `RECORD_AUDIO` — confirmar que el STT (`rememberSpeechToTextManager`) sigue activo en el wizard de NewCard; si está deshabilitado, sacarlo.
- **Criterio:** la app pide solo lo que usa. Play Console no marca permisos no justificados.
- **Estimación:** 30 min.
- **Estado:** [x] — `POST_NOTIFICATIONS` removido del manifest y el `LaunchedEffect` huérfano en `DashboardRoute` también borrado (no había notificaciones implementadas). `RECORD_AUDIO` se mantiene (STT activo en `NewCardInputStepScreen`). `READ/WRITE_EXTERNAL_STORAGE` con `maxSdkVersion=32` quedan (uso legacy < Android 13). Commit `8e8e5dd`.

---

### Sprint 2 — Antes de invitar beta testers (objetivo: 1 día)

#### S2-T1: Retry + timeout + logging en `GeminiService`
- **Archivo:** `data/src/main/kotlin/com/emm/data/flashcard/GeminiService.kt`
- **Por qué:** una caída de red o un timeout dejan al usuario sin feedback útil.
- **Qué hacer:** wrap del `generateContent` con: timeout explícito (10-15 s), retry 3 veces con backoff exponencial (1s, 2s, 4s), try-catch que capture `FirebaseException` y logue el raw response (truncado) a Crashlytics como non-fatal.
- **Criterio:** mock de red caída → usuario ve mensaje claro "no se pudo conectar, reintentando" y a los ~7s "no se pudo generar, reintentá más tarde". Crashlytics recibe non-fatal con stack del error original.
- **Estimación:** 3 h.
- **Estado:** [ ]
- **Depende de:** S1-T1.

#### S2-T2: Quality checks deterministas en Kotlin
- **Archivos:** `domain/src/main/kotlin/com/emm/domain/generation/GeneratedLearningNoteQualityChecksPolicy.kt`, `data/src/main/kotlin/com/emm/data/flashcard/Prompt.kt`
- **Por qué:** los checks actuales son auto-sellados por el modelo. Reemplazar 2-3 por validadores reales agrega valor sin reescribir todo.
- **Qué hacer:** elegir 2-3 checks con criterio determinable (ej: `required_fields_present` ya es chequeable; `single_meaning` se puede verificar con regex sobre `cards`; `natural_example` con wordlist de "textbookismos"). Removerlos del prompt y validarlos en Kotlin. Dejar el resto del prompt como hint informativo.
- **Criterio:** una nota con un campo vacío que el modelo marcó `passed: true` ahora falla validación.
- **Estimación:** 4 h.
- **Estado:** [ ]

#### S2-T3: Normalizar inputs a inglés en prompt builder
- **Archivo:** `data/src/main/kotlin/com/emm/data/flashcard/Prompt.kt`
- **Por qué:** lenguaje mezclado en el system prompt produce outputs inconsistentes.
- **Qué hacer:** en cada builder, mapear inputs en español (ej: `communicativeIntentLabel`) a su versión en inglés antes de interpolar. Si el catálogo está en español, agregar campo `englishLabel` o tabla de traducción inline.
- **Criterio:** los prompts finales que envía a Gemini están 100% en inglés (verificable con un log del prompt completo en debug).
- **Estimación:** 2 h.
- **Estado:** [ ]

#### S2-T4: Documentar `SpacedRepetitionScheduler`
- **Archivo:** `domain/src/main/kotlin/com/emm/domain/study/SpacedRepetitionScheduler.kt`
- **Por qué:** es el corazón del producto. Si un usuario reporta "esta carta volvió muy rápido", hay que poder explicar por qué.
- **Qué hacer:** comentario en el archivo explicando: variante de SM-2 usada, racional de los coeficientes del delta de ease, cómo se calcula el `nextInterval`. Si la decisión es migrar a SM-2 estándar o FSRS, decidirlo acá y trackearlo como tarea S3.
- **Criterio:** un dev externo puede leer el archivo y entender el algoritmo sin grep adicional.
- **Estimación:** 1-2 h.
- **Estado:** [ ]

#### S2-T5: Startup con timeout
- **Archivo:** `app/src/main/kotlin/com/emm/hello/newfeatures/NewRoot.kt`
- **Qué hacer:** envolver el `collect` del `AppStartupViewModel` con `withTimeoutOrNull(5_000L)`, mostrar error con retry si timeout.
- **Criterio:** simular DB corrupta (renombrar archivo SQLite en device) → la app muestra error con botón "reintentar" en <6s, no loading infinito.
- **Estimación:** 1 h.
- **Estado:** [ ]

#### S2-T6: Build release real en device + recorrido manual
- **Qué hacer:** generar APK release firmado, instalarlo en device físico, hacer recorrido completo: crear deck → crear card (los 3 modos) → estudiar → editar → eliminar → exportar backup → reinstalar → importar.
- **Criterio:** los 5 flujos funcionan en release con minify activo, sin crashes en Crashlytics, sin pérdida de datos en backup round-trip.
- **Estimación:** 2 h.
- **Estado:** [ ]
- **Depende de:** todos los S1 + S2-T1.

---

### Sprint 3 — Post-launch, con feedback real (objetivo: ongoing)

Estas tareas NO bloquean lanzamiento. Priorizar según señales de usuarios reales.

#### S3-T1: `Flashcard` con value objects
- Migrar `word/meaning/translation` a `Expression`/`IntendedMeaningEs`/`DefinitionEn`. Implica migración de schema (cards existentes deben pasar la validación de los VOs) y ajustes en mappers.
- **Estado:** [ ]

#### S3-T2: ViewModel tests por feature
- Empezar por el feature con más bugs reportados. Pattern: test de routing de intents, state updates, effect emissions.
- **Estado:** [ ]

#### S3-T3: Decidir entre SM-2 estándar / FSRS
- Validar con feedback de retención si el scheduler actual rinde. Si no, migrar a FSRS (Anki moderno) con tablas de parámetros default.
- **Estado:** [ ]

#### S3-T4: Versionar prompts
- Agregar `promptVersion` a cada nota generada. Permite A/B testing y trackear regresiones de calidad por versión de prompt.
- **Estado:** [ ]

#### S3-T5: i18n de catálogos estáticos
- `StaticCategories`, `CommunicativeIntent` con labels traducibles.
- **Estado:** [ ]

#### S3-T6: Few-shot examples en prompts de regeneración
- Agregar 1-2 ejemplos en cada builder de regeneración. Mejora consistencia con `gemini-2.5-flash`.
- **Estado:** [ ]

#### S3-T7: `UpdateFlashcardUseCase` / `SoftDeleteFlashcardUseCase` — decidir
- Si no van a tener lógica, inline en ViewModel y eliminar. Si van a tener lógica de validación, agregarla y tests.
- **Estado:** [ ]

#### S3-T8: Limpiar redundancia en docs
- Reconciliar `docs/*_CURRENT.md` con código actual post-refactor de UI.
- **Estado:** [ ]

---

## 3. Orden de ejecución recomendado

```
S1-T1 (Crashlytics)
  └─→ S1-T2 (R8 rules)
S1-T3 (modelo Gemini)
  └─→ S1-T4 (responseSchema)
S1-T5 (migrations baseline)
  └─→ S1-T6 (DeckTag cascade, si se elige schema change)
S1-T7 (privacy policy) — independiente, paralelizable
S1-T8 (manifest cleanup) — independiente

Después de S1 completo:
S2-T1 (retry/timeout/log) — depende de S1-T1
S2-T2, S2-T3, S2-T4, S2-T5 — independientes
S2-T6 (release build) — al final, depende de todo lo anterior

Después de lanzamiento:
S3-* según señales reales
```

## 4. Definición de "listo para Play Store"

Todos los items de Sprint 1 cerrados + S2-T1 + S2-T6 verificado en device real.

## 5. Documentos relacionados

- `AGENTS.md` — reglas operativas
- `ARCHITECTURE.md` — estructura técnica
- `LOCAL_FIRST.md` — contrato de runtime
- `README.md` — entry point del repo
