# Daily Notification Plan

| Field | Value |
|---|---|
| Status | Active |
| Role | Plan atómico para implementar la notificación diaria de cards due |
| Source of Truth | Yes (mientras no se cierren todas las tareas) |
| Read this when | Vas a tocar `WorkManager`, `NotificationChannel` o `Settings` para opt-in |
| Última verificación contra código | 2026-05-16 |

## TL;DR

`POST_NOTIFICATIONS` está declarado en el plan original pero **no en el manifest actual** (verificado). No hay infra de notificaciones. Esta iteración agrega: permiso, canal, worker periódico que cuenta cards due globalmente, scheduler en startup. **Sin time picker ni deep link en v1** — disparo fijo a las 19:00 local, tap abre la app en la pantalla principal.

## Decisiones explícitas

- **Disparo fijo 19:00 local** en v1. Time picker es follow-up.
- **No deep link a Study** en v1 — el `PendingIntent` abre `MainActivity`. Follow-up: deep link a deck/study.
- **Default ON** — los usuarios obtienen el reminder al instalar. Toggle off vive en Settings. Follow-up: onboarding consent dialog.
- **No backoff exponencial** — si la red/disco fallan, próxima ejecución en 24h.
- **Cuenta global de cards due**, no por deck. La notificación es genérica ("Tenés N cards para repasar hoy").
- **Skip si due_count == 0** — no molestar cuando no hay nada.

## Sprint 1 — Infra base (objetivo: 1.5 h)

### N1-T1: Manifest + permisos + ícono

- **Archivo:** `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_notification.xml` (nuevo).
- **Qué hacer:**
    1. Agregar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`.
    2. (Opcional v2) `RECEIVE_BOOT_COMPLETED` para resucitar el schedule tras reboot — WorkManager lo maneja solo si usás `PeriodicWorkRequest`, así que SKIP.
    3. Crear `ic_notification.xml` vector blanco simple (24×24). Si no, usar `R.mipmap.ic_launcher` como fallback (no recomendado por guidelines).
- **Criterio:** build pasa. La app pide permiso de notificación al primer uso (auto desde Android 13+ cuando se intenta postear).
- **Estimación:** 15 min.
- **Estado:** [ ]

### N1-T2: NotificationChannel registrado en App.onCreate

- **Archivo:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderChannel.kt` (nuevo), `App.kt`.
- **Qué hacer:**
    1. Constante `STUDY_REMINDER_CHANNEL_ID = "study_reminders"`.
    2. Función `ensureStudyReminderChannel(context: Context)` que crea el `NotificationChannel` (importance DEFAULT, lights/sound según guidelines) idempotente.
    3. Llamar desde `App.onCreate()` después de `FirebaseApp.initializeApp`.
- **Criterio:** abrir Settings > App > Notifications muestra "Recordatorios de estudio" como categoría.
- **Estimación:** 20 min.
- **Estado:** [ ]
- **Depende de:** N1-T1.

### N1-T3: Global due-cards count query + use case

- **Archivos:** `data/src/main/sqldelight/com/emm/data/Flashcard.sq` (nueva query), `domain/src/main/kotlin/com/emm/domain/flashcard/CountDueFlashcardsUseCase.kt` (nuevo), `FlashcardRepository.kt` (interface), `DefaultFlashcardRepository.kt` (impl).
- **Qué hacer:**
    1. Query SQL:
        ```sql
        countDueFlashcards:
        SELECT COUNT(*)
        FROM Flashcard f
        LEFT JOIN ReviewProjection rp
          ON f.id = rp.flashcardId
        WHERE f.deletedAt IS NULL
          AND (rp.nextReviewAt IS NULL OR rp.nextReviewAt <= :now);
        ```
    2. `FlashcardRepository.countDueFlashcards(nowMillis: Long): Long` — método nuevo.
    3. `CountDueFlashcardsUseCase(repo, clock)` — pure use case que devuelve `Long`.
    4. Test: `CountDueFlashcardsUseCaseTest` con fake repo.
- **Criterio:** test verde. Cuenta cards con `nextReviewAt IS NULL` (nuevas) más cards con `nextReviewAt <= now`.
- **Estimación:** 30 min.
- **Estado:** [ ]

### N1-T4: DueCardsReminderWorker

- **Archivo:** `app/src/main/kotlin/com/emm/hello/notifications/DueCardsReminderWorker.kt` (nuevo).
- **Qué hacer:**
    1. `class DueCardsReminderWorker(context, params, useCase, clock) : CoroutineWorker` — inyectado via Koin (`WorkerFactory` o `koin-androidx-workmanager`).
    2. `doWork()`: invoca `CountDueFlashcardsUseCase`. Si count == 0, return `Result.success()` sin notificar. Si count > 0, postea notificación con título "Tu repaso del día" y cuerpo "Tenés N cards para repasar".
    3. PendingIntent target: `MainActivity` con flags inmutables (`FLAG_IMMUTABLE`).
    4. ID de notificación constante (`STUDY_REMINDER_NOTIFICATION_ID = 1001`) → re-postea sobre la anterior si el usuario no la abre.
- **Criterio:** ejecutar el worker manualmente desde una unit-style test (o WorkManager test runner) con count > 0 produce una notificación visible.
- **Estimación:** 45 min.
- **Estado:** [ ]
- **Depende de:** N1-T2, N1-T3.

### N1-T5: Scheduler en app startup (PeriodicWorkRequest 24h, primera ejecución a las 19:00)

- **Archivo:** `app/src/main/kotlin/com/emm/hello/notifications/StudyReminderScheduler.kt` (nuevo), `App.kt` (invocación).
- **Qué hacer:**
    1. `StudyReminderScheduler.scheduleDaily()` configura `PeriodicWorkRequest<DueCardsReminderWorker>` con `repeatInterval = 24h`, `flexInterval = 1h`, `initialDelay` calculado para alinear con las 19:00 locales del día actual o siguiente.
    2. Enqueue con `ExistingPeriodicWorkPolicy.UPDATE` (no `KEEP`) para que cambios de hora se reflejen sin reinstalar.
    3. Llamar desde `App.onCreate()` después de `startKoin`.
- **Criterio:** `adb shell dumpsys jobscheduler | grep emm` muestra el job programado. Cambiar el reloj a 19:00 y la notificación aparece dentro de la ventana de 1h.
- **Estimación:** 30 min.
- **Estado:** [ ]
- **Depende de:** N1-T4.

## Sprint 2 — Opt-out + polish (objetivo: 1.5 h)

### N2-T6: Settings toggle on/off

- **Archivos:** `data/.../UserPreferences` (nuevo o extender), `app/.../settings/SettingsViewModel.kt`, `SettingsScreen.kt`.
- **Qué hacer:**
    1. Almacenar preferencia en `DataStore`/`SharedPreferences`: `study_reminder_enabled: Boolean` (default `true`).
    2. UI en Settings: `Switch` "Recordatorio diario de estudio" con sublabel "Cada día a las 19:00".
    3. Al cambiar a OFF → `WorkManager.cancelUniqueWork(...)`. Al cambiar a ON → re-encolar.
- **Criterio:** toggle off → `adb shell dumpsys jobscheduler` ya no muestra el job. Toggle on → vuelve a aparecer.
- **Estimación:** 1 h.
- **Estado:** [ ]
- **Depende de:** N1-T5.

### N2-T7: Strings i18n + ícono final

- **Archivos:** `values/strings.xml`, `values-en/strings.xml` (si existe), `res/drawable/ic_notification.xml`.
- **Qué hacer:**
    1. Extraer textos: `notification_title`, `notification_body` (plurals), `notification_channel_name`, `notification_channel_description`, `settings_study_reminder_title`, `settings_study_reminder_subtitle`.
    2. Usar `<plurals>` para "1 card" vs "N cards".
    3. Reemplazar ícono mock por uno definitivo (vector blanco, 24×24, sin background — guideline Material).
- **Criterio:** notificación renderiza el plural correcto. Cambiar locale del device respeta el idioma.
- **Estimación:** 30 min.
- **Estado:** [ ]

## Follow-ups conocidos (NO en esta iteración)

- **F-Time-Picker**: permitir al usuario elegir hora del recordatorio (no fijo 19:00). Requiere DataStore + lógica de re-cálculo del `initialDelay`.
- **F-Deep-Link**: tap en la notificación → `Study` para un deck específico (o el deck con más due).
- **F-Onboarding-Consent**: pedir permiso `POST_NOTIFICATIONS` con UI contextual durante onboarding (en Android 13+) en vez de al primer post.
- **F-Multi-Reminder**: notificaciones múltiples por deck en vez de una global (UX más rica pero más ruidoso).

## Decisiones que no son obvias

- **¿Por qué `flexInterval = 1h`?** WorkManager para `PeriodicWorkRequest` permite una ventana de flex que el sistema usa para agrupar wakes y ahorrar batería. Sin flex el sistema podría hacer wake exact, que en Doze mode no es posible. 1h es buen tradeoff: el usuario ve la notif entre 18:00 y 19:00, no estrictamente a las 19:00.
- **¿Por qué Worker en `app/` y no en `:data`?** El worker depende de notificación (Android API) que no entra en `:data` puro. Vive con UI/scaffolding.
- **¿Por qué `UPDATE` en lugar de `KEEP`?** El plan permite cambiar la hora más adelante (F-Time-Picker). `UPDATE` re-encola con la nueva spec sin requerir cancelación manual.
