# Privacy Policy — Hello

**Effective date:** 2026-05-15
**Last updated:** 2026-05-15
**App:** Hello (`com.emm.hello`)
**Contact:** edgardo.emm20@gmail.com

This policy explains what data the **Hello** Android app collects, how it is used, and with whom it is shared. Hello is a **local-first** flashcard app for learning English: your decks, cards, and study history live on your device.

---

## 1. Data we collect

### 1.1 Stored only on your device
The following data never leaves your device unless **you** explicitly export a backup:

- Decks, flashcards, tags, study history, review schedules.
- A locally generated **device identifier** (random UUID) created on first launch. It is used internally to scope local data and is **not** transmitted to any server.
- App settings and preferences.

If you uninstall the app, all of this data is deleted from the device.

### 1.2 Data sent to third parties
Hello sends data to the following Google services:

| Service | Data sent | Purpose |
|---|---|---|
| **Firebase AI (Gemini API)** | The word, phrase, or sentence you type when generating a flashcard, plus the generation parameters (e.g., target level, intent) you select. | Generate the flashcard content (translation, examples, definitions) you requested. |
| **Firebase Crashlytics** | Crash stack traces, device model, OS version, app version, anonymous installation ID. | Diagnose crashes and stability issues. |
| **Firebase Analytics** | Standard app events (e.g., `first_open`, `session_start`), device/OS metadata, anonymous installation ID. | Understand aggregate usage to prioritize improvements. |

We do **not** collect: your name, email address, contacts, location, photos, microphone audio (speech-to-text runs on-device via Android's `SpeechRecognizer`), or any account credentials. Hello has no login.

### 1.3 Backups
When you tap **Export backup**, the app produces a JSON file containing your decks, cards, tags, and study history. The file is saved wherever you choose (local storage, Drive, email, etc.). **You** control where this file goes; Hello does not upload it anywhere automatically.

When you tap **Import backup**, the app reads a JSON file you select and writes its contents to the local database.

---

## 2. How third parties use this data

- **Google Firebase AI / Gemini** processes the input you submit to generate a response and may retain it according to Google's terms. See: <https://policies.google.com/privacy> and <https://ai.google.dev/gemini-api/terms>.
- **Firebase Crashlytics** and **Firebase Analytics** retain telemetry under Google's data retention policies. See: <https://firebase.google.com/support/privacy>.

We do not sell or share your data with any other third parties.

---

## 3. Permissions

Hello requests the following Android permissions:

- **Internet** — required to call Firebase AI for flashcard generation and to send crash/analytics telemetry.
- **Record audio** (`RECORD_AUDIO`) — used only when you tap the microphone button in the new-card screen to dictate the source word. Audio is processed by Android's on-device speech recognizer and never sent by Hello to any server.
- **Read/Write external storage** (Android 12 and below only) — used to read backup files you select for import and to write backup files you export.

You can revoke any permission at any time from Android Settings.

---

## 4. Data retention and deletion

- **On-device data:** deleted when you uninstall the app or use the app's delete/clear actions.
- **Generation inputs sent to Gemini:** retained by Google per their policy.
- **Crashlytics / Analytics:** retained per Firebase defaults (Crashlytics: 90 days; Analytics: up to 14 months unless changed). To request deletion of telemetry tied to your installation, email us with your anonymous installation ID (visible in app settings if exposed) and we will request deletion via the Firebase console.

---

## 5. Children

Hello is not directed at children under 13 and does not knowingly collect data from them.

---

## 6. Changes to this policy

We may update this policy as the app evolves. Material changes will be announced in the app's release notes and reflected in the **Last updated** date at the top.

---

## 7. Contact

Questions or deletion requests:
**Edgardo Muñoz — edgardo.emm20@gmail.com**
