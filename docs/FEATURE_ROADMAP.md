# Feature Roadmap

| Field | Value |
|---|---|
| Status | Active |
| Role | Plan corto por feature pendiente. Cada entrada se expande a un doc atómico tipo `STUDY_UX_ITERATION.md` cuando se prioriza. |
| Source of Truth | Yes para prioridad y tamaño de cada item |
| Read this when | Vas a elegir qué feature implementar a continuación |
| Última verificación contra código | 2026-05-16 |

## Orden recomendado

1. **Sprint 2 de Notifications** (settings toggle + time picker) — completa la feature ya scaffoldeada.
2. **Undo soft-delete** — chico, polish UX.
3. **Búsqueda global de flashcards** — alta utilidad, mediano.
4. **Stats / racha** — engagement, mediano-grande.
5. **Tags a nivel flashcard** — schema mig, grande.
6. **Onboarding primer uso** — opcional, mediano.
7. **Modo cram (no-SRS)** — opcional, mediano.

`F2` (validación device) y `S1-T7` (privacy URL) son del usuario, no míos.

---

## 1. Notifications Sprint 2

| Field | Value |
|---|---|
| Tamaño | Mediano (~1.5 h) |
| Bloquea | Nada |
| Bloqueado por | Notifications Sprint 1 (ya hecho, `9cddeb6`) |
| Plan completo | `docs/NOTIFICATIONS_PLAN.md` Sprint 2 + Follow-ups |

**Tareas:**

- **N2-T6**: Settings toggle on/off persistido en `DataStore`. Al cambiar OFF → `StudyReminderScheduler.cancel(context)`. ON → `scheduleDaily(context)`.
- **N2-T7**: Strings i18n + ícono Material guideline-compliant (vector blanco 24×24 sin background).
- **F-Time-Picker**: TimePicker en Settings para elegir hora. Persistir como `LocalTime`. `StudyReminderScheduler.scheduleDaily(context, time)` recalcula `initialDelay`.
- **F-Deep-Link**: tap en notificación → `Study` para el deck con más cards due. Requiere extender `MainActivity` con intent extras.

**Definition of done:** usuario puede desactivar el reminder, cambiar la hora, y al tocar la notificación entra directamente al estudio del deck más cargado.

---

## 2. Undo soft-delete (Feature #12)

| Field | Value |
|---|---|
| Tamaño | Chico (~1 h) |
| Bloquea | Nada |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **U-T1**: Agregar queries `restoreByTimestamp` en `Flashcard.sq` y `FlashcardExample.sq`:
    ```sql
    restoreByTimestamp:
    UPDATE Flashcard SET deletedAt = NULL
    WHERE id = :id AND deletedAt = :timestamp;
    ```
    El filtro por timestamp es para no restaurar examples soft-deleted antes (independiente del cascade actual).
- **U-T2**: `FlashcardRepository.restoreFlashcard(flashcardId, deletedAtTimestamp)` + impl en `DefaultFlashcardRepository` con transacción que también restaura los examples cascaded.
- **U-T3**: `RestoreFlashcardUseCase`.
- **U-T4**: Modificar `FlashcardDetailViewModel.deleteFlashcard()`:
  1. Capturar `deletedAt` timestamp.
  2. Setear estado `pendingDeletion = PendingDeletion(timestamp, dismissJob)`.
  3. Lanzar coroutine con `delay(5_000)` → emite `FlashcardDeleted` (navega back).
  4. Si recibe `UndoDeletion` intent → cancel job + `RestoreFlashcardUseCase(flashcardId, timestamp)`.
- **U-T5**: UI: `Snackbar` con action "Deshacer" en `FlashcardDetailScreen` mientras `pendingDeletion != null`. Auto-dismiss en 5s.

**Caveat:** durante los 5s, la card está soft-deleted en DB pero la UI todavía la muestra (data en memoria del state). El user-flow es coherente porque la pantalla no recarga.

---

## 3. Búsqueda global de flashcards (Feature #8)

| Field | Value |
|---|---|
| Tamaño | Mediano (~2 h) |
| Bloquea | Nada |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **S-T1**: Query SQL nuevo en `Flashcard.sq`:
    ```sql
    searchFlashcards:
    SELECT f.*, d.name AS deckName
    FROM Flashcard f
    INNER JOIN Deck d ON f.deckId = d.id
    WHERE f.deletedAt IS NULL
      AND d.deletedAt IS NULL
      AND (LOWER(f.word) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(f.meaning) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(f.translation) LIKE '%' || LOWER(:query) || '%')
    ORDER BY f.createdAt DESC
    LIMIT 50;
    ```
- **S-T2**: `FlashcardRepository.search(query: String): Flow<List<FlashcardSearchResult>>` (incluye nombre del deck).
- **S-T3**: Extender `DashboardUiState`:
  - `flashcardResults: List<FlashcardSearchResult>`
  - Al escribir en `HSearchBar`, además de filtrar decks, llama `search(query)` debounced (~300 ms).
- **S-T4**: UI en Dashboard: si hay query no-vacío, mostrar dos secciones — "Decks" (lo actual) y "Flashcards" (cards matched con su deck name y un highlight del término matched).
- **S-T5**: Tap en card de búsqueda → navega a `CardDetailRoute(flashcardId)`.

**Definition of done:** con 500 cards, buscar "phrasal" muestra todas las cards con esa palabra/significado en menos de 200 ms.

---

## 4. Stats / racha (Feature #7)

| Field | Value |
|---|---|
| Tamaño | Grande (~4-5 h) |
| Bloquea | Onboarding pre-ranking |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **St-T1**: Nueva tabla `ReviewLog`:
    ```sql
    CREATE TABLE ReviewLog (
      id TEXT NOT NULL PRIMARY KEY,
      flashcardId TEXT NOT NULL,
      reviewedAt INTEGER NOT NULL,
      grade INTEGER NOT NULL
    );
    CREATE INDEX idx_ReviewLog_reviewedAt ON ReviewLog(reviewedAt);
    ```
- **St-T2**: Migration `2.sqm` para crear la tabla. Verify con `verifySqlDelightMigration`.
- **St-T3**: Insertar log entry en `ScheduleFlashcardReviewUseCase` (o en el VM antes de actualizar review). Decisión: hacerlo en el use case para mantener single source of truth.
- **St-T4**: Queries en `Stats.sq`:
  - `reviewsByDay`: COUNT(*) GROUP BY date(reviewedAt/1000, 'unixepoch')
  - `currentStreak`: cálculo en Kotlin sobre los días consecutivos con ≥ 1 review.
- **St-T5**: Domain: `GetStudyStatsUseCase` devuelve `StudyStats(streak, reviewsToday, reviewsThisWeek, accuracy30d, heatmap30d)`.
- **St-T6**: Extender `DashboardStatsSection` para mostrar racha, gráfico simple del heatmap (30 días).

**Definition of done:** Dashboard muestra "Racha: 5 días", "Hoy: 12 cards", heatmap visual de 30 días.

---

## 5. Tags a nivel flashcard (Feature #9)

| Field | Value |
|---|---|
| Tamaño | Grande (~5-6 h) |
| Bloquea | Nada |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **T-T1**: Tabla `FlashcardTag(flashcardId, tagId)` con FKs ON DELETE CASCADE. Migration `3.sqm`.
- **T-T2**: Repo methods: `addTag`, `removeTag`, `flashcardTags(flashcardId)`, `flashcardsByTag(tagId)`.
- **T-T3**: UI en `EditFlashcardScreen` para asignar tags vía `HTagInput`.
- **T-T4**: Filtro por tag en `DeckDetail` y `Dashboard` flashcard search.
- **T-T5**: Backup export/import incluye `FlashcardTag` con filtro de soft-delete (mismo patrón que `DeckTag`).

**Definition of done:** crear/editar card permite asignar tags. Filtrar deck por tag muestra solo cards taggeadas.

---

## 6. Onboarding primer uso (Feature #10)

| Field | Value |
|---|---|
| Tamaño | Mediano (~2-3 h) |
| Bloquea | Notifications opt-in dialog |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **O-T1**: Detectar primer uso en `AppStartupCoordinator` (flag persistido en DataStore).
- **O-T2**: Nueva feature `app/.../onboarding/` con 3 pantallas: bienvenida, "creá tu primer deck" (genera deck demo), opt-in de notificaciones (request POST_NOTIFICATIONS en Android 13+).
- **O-T3**: Skip-able pero con CTA prominente para crear el primer deck.
- **O-T4**: Marcar `firstRunCompleted = true` al final.

**Definition of done:** instalar fresco → onboarding aparece una vez. Cierre o completar marca el flag y no vuelve a aparecer.

---

## 7. Modo cram / no-SRS (Feature #11)

| Field | Value |
|---|---|
| Tamaño | Mediano (~2-3 h) |
| Bloquea | Nada |
| Bloqueado por | Nada |

**Tareas atómicas:**

- **C-T1**: Nuevo intent `StartCramSession` en `StudyViewModel` que carga TODAS las cards del deck (no solo due) en orden aleatorio, sin schedule actualizado.
- **C-T2**: UI: botón "Cram" en `DeckDetail` además del "Estudiar" actual.
- **C-T3**: En la sesión cram, los grade buttons existen pero NO escriben en `FlashcardReview` — solo navegan al siguiente. Banner sutil indica "Modo repaso rápido (no afecta tu progreso SRS)".
- **C-T4**: Misma UI de Study, solo cambia la fuente de items y el callback de grade.

**Definition of done:** "Cram" arranca con todas las cards aleatorias. Tocar "Good" avanza sin escribir SRS. Saliendo del cram, los intervals del deck quedan intactos.

---

## Decisiones de cierre

- **Privacy URL (S1-T7 del audit)**: bloqueado en el usuario hasta que publique `docs/privacy-policy.md` en una URL pública. Cuando me digas la URL, agrego el meta-data al manifest en 5 minutos.
- **F2 validación device**: necesito hardware tuyo. Pasame lo que detectes y refino.
- **F3 color HARD**: ya cerrado en commit `5f33a94`.
