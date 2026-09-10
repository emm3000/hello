# Current Settings

| Field | Value |
|---|---|
| Status | Active |
| Role | Factual feature reference |
| Scope | `Settings` flow (backup export/import, daily study reminder, Google account linking) |
| Source of Truth | No |
| Read this when | You need to understand exporting/importing local data, the daily study reminder, or linking a Google account |
| Last verified | 2026-09-07 |

## Summary

`Settings` lets you export local state to a file and restore the database from a backup, using the Storage Access Framework (SAF), and configure the daily study reminder (on/off, time). It's the only feature that interacts with OS `Uri`s. Enabling the reminder also gates the `POST_NOTIFICATIONS` runtime permission (Android 13+): turning it on requests the permission if not already granted, and a blocked state is surfaced directly on the reminder row. It also lets you link the local, lazily-created anonymous Supabase account to a Google account, so AI credits and study history survive a reinstall.

Layout: an `HTopBar` with only a back arrow, then a `metadata` eyebrow, a `displayMedium` headline and a subtitle; an "Organization" section whose "Decks" row opens deck management; a "Reminders" section with a single row for the daily study reminder; an "Account" section with a row for linking a Google account and an inert row showing the remaining AI generations; a "Your data" section with the export and import rows separated by an `HSeparator`; and a footer with a tagline plus a `metadata` meta line in `inkFaint`. All sections are `surface` panels shaped with `helloShapes.control`, labelled via `HSectionLabel`; each row shows `titleSmall` title, `bodySmall` subtitle and a chevron, a trailing control, or an `HLoadingSpinner` while busy. The import row's subtitle ("Replaces everything you have now.") is rendered in `destructiveInk` to signal the destructive nature of the action.

## Key files

- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsRoute.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiState.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiIntent.kt`
- `app/src/main/kotlin/com/emm/hello/newfeatures/settings/SettingsUiEffect.kt`
- `app/src/main/kotlin/com/emm/hello/core/ui/Switch.kt` (`HSwitch`)
- `app/src/main/kotlin/com/emm/hello/core/ui/TimePicker.kt` (`HTimePickerDialog`)
- `app/src/main/kotlin/com/emm/hello/notifications/NotificationPermission.kt` (port, `isGranted()`)
- `app/src/main/kotlin/com/emm/hello/notifications/SystemNotificationPermission.kt` (impl over `NotificationManagerCompat.areNotificationsEnabled()`)
- `app/src/main/kotlin/com/emm/hello/notifications/PostNotificationsRequest.kt` (`requestPostNotificationsPermission`)
- `app/src/main/kotlin/com/emm/hello/core/auth/GoogleSignInLauncher.kt` (port, `signIn(serverClientId)`)
- `app/src/main/kotlin/com/emm/hello/core/auth/ActivityGoogleSignInLauncher.kt` (impl, delegates to `GoogleCredentialClient` on the current foreground `Activity`)
- `app/src/main/kotlin/com/emm/hello/core/auth/GoogleCredentialClient.kt` (wraps Android's `CredentialManager` / Google ID token flow)
- `app/src/main/kotlin/com/emm/hello/core/auth/GoogleSignInResult.kt` (`Success` / `Cancelled` / `NoCredentials` / `Failure`)
- `app/src/main/kotlin/com/emm/hello/core/activity/CurrentActivityHolder.kt` (`Application.ActivityLifecycleCallbacks`, tracks the foreground `Activity`)

## :data / :domain dependencies

- `com.emm.data.export.BackupExporter`
- `com.emm.data.export.BackupImporter`
- `com.emm.domain.reminder.GetStudyReminderSettingsUseCase`
- `com.emm.domain.reminder.SetStudyReminderEnabledUseCase`
- `com.emm.domain.reminder.SetStudyReminderTimeUseCase`
- `com.emm.domain.account.GetAccountUseCase`
- `com.emm.domain.account.LinkGoogleAccountUseCase`
- `com.emm.domain.account.AccountRepository` (interface), implemented by `com.emm.data.remote.SupabaseAccountRepository`
- `com.emm.domain.generation.GenerationCreditsRepository` (interface), implemented by `com.emm.data.generation.DefaultGenerationCreditsRepository`
- `com.emm.domain.time.Clock`

## State

`SettingsUiState`:

- `isExporting`
- `isImporting`
- `isConfirmDialogVisible`
- `pendingImportUri: Uri?`
- `isReminderEnabled: Boolean` — default `true`
- `reminderTime: LocalTime` — default `StudyReminderSettings.DEFAULT_TIME` (19:00)
- `isReminderTimePickerVisible: Boolean`
- `isNotificationPermissionGranted: Boolean` — default `true`; overwritten in `init` with `NotificationPermission.isGranted()`
- `account: Account?` — default `null`; loaded asynchronously in `init` — see "Account flow"
- `isLinkingAccount: Boolean` — default `false`
- `generationCredits: GenerationCredits?` — default `null`; the last reading the backend reported, or `null` once its `resetAt` has passed — see "AI generations row"
- `buildInfo: BuildInfo` — injected, never mutated

## Intents

`SettingsUiIntent`:

- `ExportData` → emits `LaunchExportPicker`
- `ImportData` → emits `LaunchImportPicker`
- `ExportUriReceived(uri)` → runs the export
- `ImportUriReceived(uri)` → stores `pendingImportUri` and opens the confirmation dialog
- `ConfirmImport` → runs the import
- `CancelImport` → clears `pendingImportUri` and closes the dialog
- `SetReminderEnabled(isEnabled)` → turning it off persists the flag directly; turning it on checks `NotificationPermission.isGranted()` first — see "Reminder flow"
- `EditReminderTime` → shows the time picker dialog
- `DismissReminderTimePicker` → hides it without saving
- `SetReminderTime(time)` → persists the time, syncs the scheduler, and hides the dialog
- `NotificationPermissionSettled` → re-reads `NotificationPermission.isGranted()` after the system permission dialog closes; if granted, also enables and persists the reminder
- `RefreshNotificationPermission` → re-reads `NotificationPermission.isGranted()` and updates state; sent by the `Route` on `ON_RESUME`
- `OpenNotificationSettings` → emits `OpenNotificationSettings`
- `LinkGoogleAccount` → starts the Google sign-in flow — see "Account flow"

## Export flow

1. User taps the export row → `ExportData` → `LaunchExportPicker`.
2. The `Route` launches `ActivityResultContracts.CreateDocument("application/json")` with a `hello-backup-<millis>.json` suggestion.
3. A resolved `Uri` comes back as `ExportUriReceived(uri)`; a cancelled picker sends nothing.
4. `SettingsViewModel`:
   - `isExporting = true`
   - `BackupExporter.export(uri)`
   - `onSuccess` → `ShowSuccess("Backup exported successfully")`
   - `onFailure` → `ShowError("Couldn't export the backup")` + log
   - `isExporting = false`

## Import flow

1. User taps the import row → `ImportData` → `LaunchImportPicker`.
2. The `Route` launches `ActivityResultContracts.OpenDocument()` filtered to `application/json`.
3. A resolved `Uri` comes back as `ImportUriReceived(uri)`, which stores `pendingImportUri` and shows the `HAlertDialog` ("Replace all data?", dangerous, confirm "Replace").
4. User confirms:
   - `ConfirmImport` → closes the dialog, `isImporting = true`, `BackupImporter.import(uri)`
     - `onSuccess` → `ShowSuccess("Backup restored")`
     - `onFailure` → `ShowError(humanizeImportError(error))` + log; `humanizeImportError` returns a fixed string — `IncompatibleSchemaException` (or a cause of it) maps to "This backup was created with another version of the app. Update the app and try again.", all other errors map to "Couldn't restore the backup.". Raw `error.message` is never surfaced to the UI.
     - then `isImporting = false`, `pendingImportUri = null`
   - `CancelImport` → clears `pendingImportUri` and closes the dialog

## Reminder flow

A "Reminders" section sits between "Organization" and "Your data": one row with a bell icon, title "Daily study reminder", a trailing `HSwitch`, and a subtitle that depends on `isNotificationPermissionGranted`:

- **Granted:** subtitle "Every day at HH:mm" (the current `reminderTime`, 24h format), muted tone. Tapping the row (outside the switch) opens `HTimePickerDialog`.
- **Blocked:** subtitle `settings_notifications_blocked` ("Notifications are blocked. Tap to allow them in system settings."), rendered in `destructiveInk`. Tapping the row calls `OpenNotificationSettings` instead of opening the time picker.

The switch itself always toggles the reminder, independent of the row tap target.

`SettingsViewModel.init` loads the current settings via `GetStudyReminderSettingsUseCase` and seeds `isReminderEnabled` / `reminderTime`, and also reads `NotificationPermission.isGranted()` to seed `isNotificationPermissionGranted` — before the screen is shown, no loading state for this section.

- **Toggle off:** `SetReminderEnabled(false)` persists the flag directly (`SetStudyReminderEnabledUseCase(false)`) and sets `isReminderEnabled = false`. No permission check.
- **Toggle on:** `SetReminderEnabled(true)` first checks `NotificationPermission.isGranted()`.
  - If granted, persists the flag (`SetStudyReminderEnabledUseCase(true)`) and sets `isReminderEnabled = true`.
  - If not granted, the flag is **not** persisted and `isReminderEnabled` stays `false`; the ViewModel emits `RequestNotificationPermission` instead. The `Route` launches the system `POST_NOTIFICATIONS` prompt via `ActivityResultContracts.RequestPermission()` through `requestPostNotificationsPermission`; below Android 13 there is no prompt, so the `Route` dispatches `NotificationPermissionSettled` directly.
- **After the system dialog closes:** the `Route` always sends `NotificationPermissionSettled`, regardless of the launcher's own `Boolean` result (that result is ignored on purpose). The ViewModel re-reads `NotificationPermission.isGranted()` directly:
  - Granted → `isNotificationPermissionGranted = true`, and the reminder is now enabled and persisted (`SetStudyReminderEnabledUseCase(true)`, `isReminderEnabled = true`).
  - Denied → `isNotificationPermissionGranted = false`, the switch stays off, and the row shows the blocked subtitle.
- **Opening system settings:** `OpenNotificationSettings` (row tap while blocked) emits the `OpenNotificationSettings` effect; the `Route` starts `Settings.ACTION_APP_NOTIFICATION_SETTINGS` for the app's package.
- **Resuming the screen:** `SettingsRoute` re-sends `RefreshNotificationPermission` on `Lifecycle.Event.ON_RESUME` via `LifecycleEventEffect`, so returning from system settings (or from any permission change) re-reads `NotificationPermission.isGranted()` and updates the row without extra user action.
- **Existing install, reminder already ON but notifications later blocked:** the switch still shows ON (`isReminderEnabled` is untouched) while the row shows the blocked subtitle, because `isNotificationPermissionGranted` is independent, read-only state. The stored preference is never mutated by a permission check alone.
- **Time:** `EditReminderTime` shows `HTimePickerDialog` (24h, Save/Cancel) seeded with the current `reminderTime`. Confirming sends `SetReminderTime(time)` → `SetStudyReminderTimeUseCase(time)`, which persists the time and also runs `SyncStudyReminderUseCase`, then hides the dialog. `DismissReminderTimePicker` hides it without saving. This flow is unaffected by the permission state.
- **Sync contract:** `SyncStudyReminderUseCase` reads `StudyReminderSettingsRepository.get()` and calls `StudyReminderScheduler.schedule(time)` when enabled, or `StudyReminderScheduler.cancel()` when not. The same use case also runs once on every app launch (`App.onCreate()`), so the schedule is re-pinned even if it was never touched in this session — see `docs/NOTIFICATIONS_PLAN.md` for the scheduler mechanics.
- **Persistence:** `DataStoreStudyReminderSettingsRepository` (`:data`) stores the flag and the hour/minute as three separate `SharedPreferences` entries through the `DataStore` wrapper (`STUDY_REMINDER_ENABLED`, `STUDY_REMINDER_HOUR`, `STUDY_REMINDER_MINUTE`), not a serialized `LocalTime`.
- **Permission port:** `NotificationPermission.isGranted()` (`com.emm.hello.notifications`) is implemented by `SystemNotificationPermission` over `NotificationManagerCompat.from(context).areNotificationsEnabled()`, and wired as a Koin `single` in `NewModule.kt`.
- **Tests:** `SettingsViewModelTest` has 8 tests covering this flow (31 total in the file).

## Account flow

An "Account" section sits between "Reminders" and "Your data": one row with an `AccountCircle` icon, title "Google account" (`settings_google_account_title`), and a subtitle that depends on the loaded `account`:

- **Anonymous, or not loaded yet** (`account == null` or `account.isAnonymous`): subtitle `settings_google_account_not_linked` ("Not linked — your cards live only on this device").
- **Linked** (`account.isAnonymous == false`): subtitle is `account.email` when present, otherwise the fallback `settings_google_account_linked` ("Linked").

The row keeps the default `ChevronTrailing`. While `isLinkingAccount` is `true` it shows an `HLoadingSpinner` instead of the trailing icon and the row is disabled (`clickable(enabled = !isBusy)`), the same mechanism the export/import rows use for `isExporting` / `isImporting`.

`SettingsViewModel.init` calls `loadAccount()` in `viewModelScope`, which reads `GetAccountUseCase()` and sets `account`. `SupabaseAccountRepository.currentAccount()` calls `auth.awaitInitialization()` before `auth.currentUserOrNull()`, so the read waits for session restoration to finish first — without that wait, a cold start could read no session yet and report an already-linked account as "not linked".

- **Tapping the row** → `LinkGoogleAccount` → `requestGoogleSignIn()`:
  - `isLinkingAccount = true`.
  - `googleSignInLauncher.signIn(googleServerClientId)` (port `GoogleSignInLauncher`, impl `ActivityGoogleSignInLauncher`) drives Android's Credential Manager Google ID flow (`GoogleCredentialClient`) on the current foreground `Activity`, tracked by `CurrentActivityHolder` (an `Application.ActivityLifecycleCallbacks` registered in `App.onCreate()`). A random nonce is generated and SHA-256-hashed for the request; the raw nonce comes back with the ID token for later verification.
  - `GoogleSignInResult.Success(idToken, rawNonce)` → `linkGoogleAccount(idToken, rawNonce)`:
    - `LinkGoogleAccountUseCase(idToken, rawNonce)` → `SupabaseAccountRepository.linkGoogleAccount` calls `sessionInitializer.ensureSession()` then `auth.linkIdentityWithIdToken(provider = Google, idToken) { nonce = rawNonce }` — **never** `auth.signInWith(IDToken)`, which would mint a fresh user and orphan the anonymous one. The Supabase user id is unchanged before and after linking, so AI credits and study history survive.
    - On success: `account` is updated, `isLinkingAccount = false`, `ShowSuccess("Google account linked")`.
    - On any other exception (`CancellationException` is rethrown): logged, `isLinkingAccount = false`, `ShowError("Couldn't link your Google account")`.
  - `GoogleSignInResult.Cancelled` (user dismissed the credential sheet) → `isLinkingAccount = false`, no effect.
  - `GoogleSignInResult.NoCredentials` → `isLinkingAccount = false`, `ShowError(noGoogleAccountMessage)` = "No Google account on this device" (`settings_google_no_credentials`).
  - `GoogleSignInResult.Failure` (includes "no foreground `Activity`") → `isLinkingAccount = false`, `ShowError(googleLinkFailedMessage)` = "Couldn't link your Google account" (`settings_google_link_failed`).
- **DI:** `googleServerClientId` comes from `R.string.default_web_client_id`, emitted into `google-services.json` because the CI fixture now declares a web OAuth client too; `noGoogleAccountMessage` and `googleLinkFailedMessage` are read from string resources and injected as plain `String`s so the ViewModel stays free of `Context` / `R`. Wired in `NewModule.kt`.
- **Tests:** `SettingsViewModelTest` has 7 tests covering this flow (37 total in the file).

## AI generations row

A second, inert row in the "Account" section shows how many AI generations are left today and the local time they renew (`settings_generation_credits_title` / `settings_generation_credits_subtitle`). It is `enabled = false` and has no trailing icon: it answers a question, it does not start anything.

Both numbers come from the server, never from the device. Every reply from the Hello backend that knows the balance carries `meta.credits_remaining` and `meta.reset_at`, and `CreditsMetaReader.readOrNull` lifts them into a `GenerationCredits(remaining, resetAt)` that the remote repositories hand to `GenerationCreditsRepository.record` **before** the reply status is mapped. That covers the `402 credits_exhausted` reply as well as a `200`, so a spent day shows zero instead of the last positive number it happened to see. A reply without `meta` — `401`, `400`, `503 credits_unavailable` — records nothing and leaves the previous reading in place.

`DefaultGenerationCreditsRepository` persists the pair through `DataStore.generationCredits` as a single `SharedPreferences.edit { }` writing `GENERATION_CREDITS_REMAINING` and `GENERATION_CREDITS_RESET_AT`; a store missing either key reads back as `null`. There are no sentinel values.

Freshness is one comparison, `now < resetAt` (`GenerationCredits.isFreshAt`), with `now` read from the injected `Clock`. Nothing is derived from the device's calendar day: the reset instant the server sent is the only boundary. `SettingsViewModel` collects `observe()` and stores the reading only while it is fresh, so a stale one renders the same "unknown" subtitle as no reading at all. The screen formats `resetAt` in the device zone and never checks freshness itself.

`SuggestViewModel` reads the same repository and caps how many words can be selected at `remaining`; an unknown or stale reading caps nothing.

## Effects

`SettingsUiEffect`:

- `ShowSuccess(message)` — snackbar
- `ShowError(message)` — snackbar
- `LaunchExportPicker` — the `Route` opens the SAF create-document picker
- `LaunchImportPicker` — the `Route` opens the SAF open-document picker
- `RequestNotificationPermission` — the `Route` launches the system `POST_NOTIFICATIONS` prompt
- `OpenNotificationSettings` — the `Route` opens `Settings.ACTION_APP_NOTIFICATION_SETTINGS` for the app

## MVI notes

`SettingsViewModel` follows the pure `onIntent(intent)` contract. The SAF pickers are side effects: the VM asks for them through `LaunchExportPicker` / `LaunchImportPicker`, and the `Route` feeds the resulting `Uri` back as `ExportUriReceived` / `ImportUriReceived` intents. `EditReminderTime`, `DismissReminderTimePicker` and `SetReminderTime` never emit an effect — they only call a use case and/or update state directly. `SetReminderEnabled` conditionally emits `RequestNotificationPermission` (turning on while blocked); `OpenNotificationSettings` (intent) always emits `OpenNotificationSettings` (effect); `NotificationPermissionSettled` and `RefreshNotificationPermission` never emit an effect, they only re-read the port and update state. The permission round trip follows the same effect-in / intent-back shape as the SAF pickers: the VM asks for the system dialog, the `Route` shows it and reports back once it's settled, and the VM never touches `Context` directly. `SettingsViewModel` takes four extra constructor dependencies for this (`GetStudyReminderSettingsUseCase`, `SetStudyReminderEnabledUseCase`, `SetStudyReminderTimeUseCase`, `NotificationPermission`) and loads the initial reminder settings and permission state in `init`. It takes six more for the account flow (`GetAccountUseCase`, `LinkGoogleAccountUseCase`, `GoogleSignInLauncher`, the server client id and two failure-message strings) and loads the account asynchronously in `init` too, via `viewModelScope`. `LinkGoogleAccount` breaks the effect-in/intent-back pattern used by the SAF pickers and the permission prompt: the `GoogleSignInLauncher` port runs the whole credential exchange itself (it needs a foreground `Activity`, supplied by `CurrentActivityHolder` rather than the `Route`) and returns a typed `GoogleSignInResult` straight to the ViewModel, so `SettingsRoute` has no involvement beyond forwarding the `LinkGoogleAccount` intent. There are no extra VM entry points.
