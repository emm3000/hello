# Privacy Policy — Hello

**App:** Hello (`com.emm.hello`)
**Contact:** edgardo.emm20@gmail.com
**Last updated: 2026-09-08**

## 1. Summary

Hello is a local-first flashcard app for learning English. Your decks, flashcards, study history and settings are stored in a database on your device.

Data that stays on your device: every deck, flashcard, example, tag, review event and scheduling record, your reminder settings, your default deck, and a locally generated device identifier. None of it is uploaded by the app.

Data that leaves your device: only the text you submit when you ask the app to generate a flashcard, the generation parameters you choose, and the list of your most recent card words when you ask for word suggestions. That text is sent to our backend, which forwards it to a third-party AI provider and returns the generated note. Crash reports and basic app measurement data are also sent to Firebase.

Data we never collect: your name, your postal address, your location, your contacts, your photos, your financial information, and any password. The app has no sign-up flow and no password. An email address becomes visible to us only if you choose to link a Google account from Settings.

## 2. Data stored only on your device

The following is written to the app's local database and to the app's private preferences, and is not transmitted anywhere by Hello:

- Decks, flashcards, card examples, tags and deck-to-tag links.
- Study history: every review event you record and the scheduling state derived from it (the FSRS interval, stability, difficulty, lapses, next review date).
- Settings: your default deck, whether the study reminder is on, and the reminder time.
- A local device identifier and install identifier, both random UUIDs generated on first launch. They exist to scope local records and are not sent to any server.

This data leaves your device only if you choose to export a backup. In Settings, "Export data" writes a JSON file containing your decks, flashcards, examples, tags, deck tags, review events and scheduling records to a location you pick through the Android file picker. You decide where that file goes. Hello does not upload it anywhere. "Restore from backup" reads a JSON file you select and writes its contents into the local database.

If you uninstall the app, the local database and preferences are removed with it.

## 3. Data sent to our backend

Our backend runs on Supabase (hosted by Supabase). It consists of two functions, `generate-note` and `suggest-words`, and a small database.

### 3.1 Anonymous account

The first time you use an AI feature, the app creates an anonymous account with Supabase Auth. This account has no email address, no name and no password. It is identified only by a randomly assigned user id. It is created lazily, so if you never use an AI feature, no account is created.

You may optionally link a Google account from Settings, under "Account". If you do, the email address of that Google account becomes attached to your Supabase account and is displayed in Settings. Linking is entirely optional; the app works without it.

### 3.2 Generation requests

When you ask Hello to generate a flashcard, the app sends to `generate-note`:

- the text you typed or dictated,
- the input type, the learning goal, the level band, the register and the domain you selected,
- when the app retries after a failed validation, the codes and field names of the previous validation issues.

When you ask Hello for word suggestions, the app sends to `suggest-words` a short list of the words from your most recently created cards. No other card content is sent.

Each request also carries the session token of your anonymous account and a Firebase App Check token, which are used to confirm the request comes from a genuine installation of the app.

### 3.3 Note cache

Generated notes are cached on our backend so that an identical request does not trigger a new AI call. The cache key is a SHA-256 hash of the request text in lower case together with the generation parameters and the prompt and schema versions. The cache row stores the generated note, whether the generation succeeded, the provider and model that produced it, the version numbers and a hit counter. It does not store your user id, so cache entries are not linked to you.

Cache entries for successful generations are kept without an expiry date. Cache entries recording a refusal expire 24 hours after they are written.

### 3.4 Credit ledger

To keep the free AI quota fair and to protect the service from abuse, our backend records one row per generation attempt in a table of generation events. Each row holds your Supabase user id, which operation was requested, the cache key of the request, the provider and model used, whether the answer came from the cache, the outcome of the attempt, and a timestamp. It does not hold the text you submitted.

These rows are counted per day to enforce a daily allowance of generations and a daily allowance of refusals, both of which reset at midnight UTC.

### 3.5 Backend logs

The functions write operational log lines containing the operation name, whether the answer was cached, the outcome, the elapsed milliseconds, and for each provider attempt the provider id, the model, the HTTP status and the latency. When a provider returns output that fails validation, a truncated error detail is logged with quoted substrings removed. These log lines do not contain the text you submitted.

## 4. AI providers

Our backend forwards your submitted text and the generation parameters to third-party AI providers, trying them in this order until one succeeds:

| Provider | Endpoint | Purpose | Privacy terms |
|---|---|---|---|
| Groq | api.groq.com | Generate the learning note or the word suggestions | <https://groq.com/privacy-policy/> |
| Google Gemini API | generativelanguage.googleapis.com | Same, when the first provider is unavailable | <https://ai.google.dev/gemini-api/terms> and <https://policies.google.com/privacy> |
| OpenRouter | openrouter.ai | Same, when the previous providers are unavailable | <https://openrouter.ai/privacy> |

Only the text you submitted and the generation parameters are forwarded, inside a prompt. Your local database is never forwarded. Your account id, your device identifier and your study history are never forwarded. Each provider processes that text under its own terms, linked above.

## 5. Firebase services

The app uses three Google Firebase services.

- **Firebase Crashlytics.** Collection is enabled in the app. When the app crashes or when an AI response fails to parse, Crashlytics receives a stack trace, the device model, the operating system version, the app version and a Firebase installation identifier. In the specific case of a response that fails to parse, the report also includes up to 8,000 characters of the raw response body received from the backend, which contains the generated note derived from the text you submitted.
- **Firebase Analytics.** The app initializes Analytics and does not log any custom events. Analytics therefore collects only the automatically collected events and parameters that the Firebase SDK gathers by default, together with a Firebase installation identifier and basic device and app metadata.
- **Firebase App Check.** Release builds use the Play Integrity provider; debug builds use the debug provider. App Check produces a short-lived attestation token that is sent with every backend request so the backend can reject traffic that does not come from a genuine installation of the app. It attests the app installation, not you.

Firebase privacy information: <https://firebase.google.com/support/privacy>.

## 6. Permissions

Hello declares these Android permissions and no others.

- **Internet** and **Network state.** Required to reach our backend for flashcard generation and word suggestions, to send crash and analytics data, and to check whether a connection is available before attempting a request.
- **Microphone (RECORD_AUDIO).** Used only when you tap the microphone control to dictate a word or phrase. Recognition is performed by the Android system speech recognition service through the platform speech recognizer. Hello receives only the recognized text; it never records, stores or transmits audio itself. The system speech recognition service on your device is provided by Android or by the recognizer app you have installed, and it may process the audio on the device or on its own provider's servers according to that provider's policy.
- **Notifications (POST_NOTIFICATIONS, Android 13 and above).** Used only for the daily study reminder. The reminder is scheduled on your device and counts your due cards from the local database. No notification content leaves the device.
- **Read and write external storage (Android 12 and below only).** Present for compatibility with older Android versions when reading a backup file you select and writing a backup file you export. On current Android versions the app uses the system file picker and does not need these permissions.

The app also queries the device for a text-to-speech service and a speech recognition service so it can read cards aloud and accept dictation. Text you ask the app to pronounce is passed to the text-to-speech engine installed on your device.

You can revoke any runtime permission at any time from Android Settings.

## 7. Data retention

- **On-device data.** Kept until you delete it in the app or uninstall the app.
- **Anonymous account.** Kept while the account exists, so that your daily allowance and any linked Google identity remain associated with your installation. Deleted on request.
- **Generation events (the credit ledger).** No automatic expiry is configured. These rows are retained for service operation, quota enforcement and abuse prevention, and are deleted on request.
- **Note cache.** Successful entries have no expiry and are retained for service operation. Refusal entries expire 24 hours after they are written. Cache entries are not linked to a user id.
- **Backend logs.** Retained by the hosting platform under its own default retention for function logs. They contain no submitted text.
- **Crashlytics.** Crash reports are retained for 90 days under Firebase defaults.
- **Analytics.** Retained under Firebase default retention settings for automatically collected data.

## 8. Your rights and how to request deletion

You can delete your local data at any time from inside the app, or by uninstalling it.

To request deletion of the data held on our backend, email edgardo.emm20@gmail.com from the address you used if you linked a Google account, or describe your installation if you did not. The app does not display your anonymous account id, so if no Google account is linked we may not be able to identify your rows with certainty; in that case tell us the approximate dates on which you used the AI features and we will delete what we can identify.

To request deletion of crash and analytics data tied to your installation, email the same address and we will submit the deletion request through the Firebase console.

## 9. Children

Hello is not directed at children under 13 and does not knowingly collect data from them. If you believe a child has provided data through the app, contact us and we will delete it.

## 10. Changes to this policy

We may update this policy as the app changes. Material changes are reflected in the "Last updated" date at the top of this document and announced in the app's release notes.

## 11. Contact

Edgardo Muñoz — edgardo.emm20@gmail.com
