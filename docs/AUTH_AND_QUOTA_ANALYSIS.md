# Auth and Quota Analysis

| Field | Value |
|---|---|
| Status | Analysis, 2026-09-09. Attestation fixed and device-verified; sign-in fallback implemented and device-verified |
| Role | Postmortem of the Play-build attestation failure, plus the account and quota model it exposed |
| Source of Truth | No. The code and the Firebase / Supabase consoles win |
| Read this when | You touch App Check, Google Sign-In, account linking or the daily allowance |

## Read this first

The first Play internal-testing build `1.0.0 (1)` failed twice: AI generation never reached the backend, and Google Sign-In reported "no Google account on this device". One root cause, fixed in the consoles, no rebuild. Fixing it exposed a third problem that is still open.

| Problem | State | Where |
|---|---|---|
| Play could not attest the app | Fixed: Cloud project linked, Play signing certificate registered in Firebase | Section 1 |
| A Google account that already exists in Supabase cannot sign in | Fixed: falls back to `signInWithIdToken`; the row goes inert once linked. Verified on the emulator: a fresh anonymous user signing in with an existing Google account lands on the existing uuid | Section 2 |
| Switching or signing out of a linked account | Out of scope until sync; rules written down | Section 2, "Future" |
| The quota is per install, resets at 19:00 Lima | Known, accepted for now | Section 3 |

Next action: the "Soon" items in section 4.

## 1. The attestation failure

### Symptoms

| Surface | What the user saw | What the log said |
|---|---|---|
| AI generation | Card stuck on "Preparing…" | `FirebaseException: code: 403 body: App attestation failed.` at `NetworkClient.exchangeAttestationForAppCheckToken` |
| Google Sign-In | "no Google account on this device" | `Auth.Api.Credentials: [GetGoogleIdOperation] Operation failed. cpwk: [28444]` |

`generate-note` had no Edge Function logs at all for the day, which placed the failure on the client, before the HTTP call.

### Root cause

Two independent misconfigurations, discovered in this order.

**The Cloud project was not linked.** Play Console reported "Play Integrity API is not integrated", 0 of 7 services. Play could not issue verdicts. The first link attempt selected a Cloud project numbered `263117607705`, which is not the project Firebase uses; the correct one is `815824425834`, the `project_number` in `app/google-services.json`.

**The registered signing fingerprints were wrong.** Once Play started issuing verdicts, Firebase still refused to exchange them. The APK pulled from a device that installed from Play (`installerPackageName=com.android.vending`) is signed by exactly one certificate, with no key rotation:

```
SHA-1    93:8A:7C:E1:13:D6:BD:43:7E:EC:B6:DA:DA:38:4E:66:4C:C0:AF:C8
SHA-256  A9:68:39:32:99:42:A9:21:03:70:23:E4:D3:86:F9:57:C4:DA:9B:43:B3:23:78:E1:AB:CE:AC:B3:10:74:55:44
DN       CN=Android, OU=Android, O=Google Inc.   RSA 4096
```

Neither was registered. Firebase held `13:01:D2:C9:…:CD:54:56` (SHA-1) and `CB:0D:15:EF:…:11:00:00` (SHA-256), which match no certificate in this project. They were pasted from a clipboard proxy driven by pixel-coordinate clicks on the Play Console, while a DOM read of the same page had already returned the correct `A9:68:39:32:…`. Nothing compared the two.

**Rule for next time:** when a value is obtained twice by different means, compare them and stop on mismatch. A value that lands in the clipboard after a click by coordinate is unverifiable; the DOM read is the one to trust.

### The certificate map

| Fingerprint | Certificate | Needed for |
|---|---|---|
| `91:4b:06:34:…:74:18:10` | `~/.android/debug.keystore` | Sign-In on debug builds |
| `73:27:ff:45:…:11:e3:22` / `d9:86:b0:9b:…:26:d4:f6` | Upload key (`CN=Edgardo, OU=emm, L=aqp, C=pe`) | Sideloaded release builds; Play accepts the upload |
| `93:8a:7c:e1:…:c0:af:c8` / `a9:68:39:32:…:74:55:44` | Play App Signing key | Everything installed from Play |

Play strips the upload signature and re-signs with its own key, so the upload key never reaches a user's device. App Check validates the certificate that is actually installed.

**To read the installed certificate:** `keytool -printcert -jarfile` returns nothing (the APK has no v1 signature), and plain `apksigner verify --print-certs` fails with `Malformed public key: ML-DSA KeyFactory not available` because Play now adds a v3.2 hybrid post-quantum block for minSdkVersion 37. Cap the range instead:

```
apksigner verify --print-certs --min-sdk-version 24 --max-sdk-version 36 base.apk
```

### Result

Both fixed and verified on a Xiaomi 25078RA3EL running the Play build. AI generation succeeds, the logcat contains no App Check error, and `GetGoogleIdOperation` now reports `Operation succeeded`. No rebuild was required — certificate resolution is server-side.

## 2. The sign-in dead end

With attestation fixed, Google Sign-In reached Supabase and failed there:

```
E SettingsViewModel: linkGoogleAccount:error identity_already_exists
```

`AccountRepository` exposes one operation, `linkGoogleAccount`, and `SupabaseAccountRepository` implements it with a single call to `auth.linkIdentityWithIdToken`. When that Google identity already belongs to another Supabase user, the call is refused and the UI shows `settings_google_link_failed` with no way forward.

### Diagnosis

Configuration was ruled out first: manual linking is enabled, the Google provider is enabled, its `Client IDs` list contains the web client `815824425834-te28dnlpmjkjpra8i5p4dehgatg5ns8h.apps.googleusercontent.com` (which is the `aud` of the token, because `GetGoogleIdOption.setServerClientId` receives `default_web_client_id`), `Skip nonce checks` is off, and the app's raw/hex-hashed nonce pair matches what Supabase expects.

The cause was data. The project already held a non-anonymous user from an earlier test:

```
88b8af3b-e6e8-4deb-b603-2b44fe629442
app_metadata {"provider":"google","providers":["google"]}
created 2026-09-08T03:37:10Z
```

Linking a second, unused Google account succeeded, which confirmed the diagnosis. That link upgraded the anonymous user in place — same uuid, `is_anonymous` flipped to false — so its generation history and daily consumption carried over. That is the desired behavior for a first link.

**This is a real bug, not test noise.** Any user whose Google account already exists in Supabase hits the same wall: a reinstall, a second device, or anyone who signed in once before. The app can link, but it cannot sign in.

### Specification

The Settings row stays "Link Google account" and only acts while the user is anonymous. Under the hood it becomes link-or-sign-in, and the user is told which one happened. Once linked, the row is inert: it shows the email and does nothing on tap.

| Current user | Google identity | Behavior | User sees |
|---|---|---|---|
| Anonymous | Unused | Link. Same uuid, history and allowance carry over | "Linked" (today's path) |
| Anonymous | Owned by another user | Sign in to that user. The anonymous user is abandoned with its consumption for the day | "Signed in as `<email>`" |
| Linked | Any | Nothing. The row is not clickable | The linked email, as today |

Decisions inside that table:

- **A linked user cannot switch from this row.** Today `AccountSection` in `SettingsScreen.kt` keeps `onClick` wired regardless of `account.isAnonymous`, so a linked user can pick a second Google account and land in the refused-link path. Disabling the tap removes that whole branch instead of deciding what a switch means. The cost is that a user who linked the wrong Google account cannot correct it without clearing app data; with one tester and a 50-a-day allowance that is acceptable.
- **Detect by code, not by message.** supabase-kt 3.8.0 throws `AuthRestException` whose `errorCode` is `AuthErrorCode.IdentityAlreadyExists`. String matching on `identity_already_exists` is the fallback only if the code is ever null.
- **The fallback reuses the same `idToken` and `rawNonce`.** A refused link does not consume the nonce; Supabase only checks that the nonce hashed into the token matches the raw one it receives. Verified on device 2026-09-09.
- **The port changes shape.** `linkGoogleAccount` returns `Account` today, which cannot tell the UI whether it linked or switched. Return a result with two cases (linked, switched to existing) so `SettingsViewModel` can pick the message. The domain stays JVM-only; the Supabase error mapping lives in `:data`.
- **Local cards are untouched in every row.** Cards never leave `HelloDb`. Abandoning an anonymous user costs at most one day of allowance, which is immaterial against 50.

Falsifier: a `SupabaseAccountRepositoryTest` case where the fake `Auth` throws `AuthRestException` with `IdentityAlreadyExists` on link and the test asserts `signInWithIdToken` is called with the same token and nonce. A `SettingsScreen` check that the row ignores taps when `account.isAnonymous == false`. Device check: sign in on a second install with the owner's primary Gmail and confirm the uuid becomes `88b8af3b`.

### Future: switching accounts

Deliberately out of scope until account sync exists. Today the backend holds nothing a user would miss (AI cache, credits, generation events), so there is no data question to answer. Sync changes that: switching accounts would mean a different set of cards, and the decision below becomes a real one.

When that day comes, do not reuse the "Link" row. Add explicit actions with their own copy:

| Action | Behavior | Copy must say |
|---|---|---|
| Sign out | Drop the session, start a fresh anonymous user | What happens to local cards and to today's allowance |
| Switch account | Sign out, then run the anonymous flow above with the new Google credential | That the current account is left behind, not merged |
| Wrong account linked | Same as switch account | Nothing extra; it is the same action |

Two rules to carry into that work:

- Never switch a user away from a linked account silently. A button that says "Link" must not sign out.
- Never merge two Supabase users. If two accounts must become one, that is a data migration with its own spec, not a side effect of sign-in.

The link-or-sign-in code path from this section is the one "Switch account" will call after signing out, so build it as a reusable use case, not as a branch inside `SettingsViewModel`.

### A promise the copy does not keep

```
settings_google_account_not_linked
"Not linked — your cards live only on this device"
```

That implies linking changes where cards live. It does not. The backend stores the AI cache, credits and generation events; cards never leave `HelloDb`. Until sync exists, the string should say what linking actually does, for example:

```
"Not linked — your daily AI allowance is tied to this install"
```

## 3. How the quota works

`reserve_generation` (`supabase/migrations/20260907154000_credits_reservation.sql`) decides every call, keyed on the Supabase user uuid:

```sql
select count(*) filter (where outcome in ('success', 'pending')),
       count(*) filter (where outcome = 'refusal')
from public.generation_events
where user_id = p_user_id
  and cached = false
  and created_at >= p_day_start;
```

| Property | Value |
|---|---|
| Daily allowance | 50 (`DAILY_ALLOWANCE` secret; `DEFAULT_DAILY_ALLOWANCE = 5` in `credits.ts` is only the fallback) |
| Daily refusal allowance | 10 (`DAILY_REFUSAL_ALLOWANCE`, same fallback value in code) |
| Cache hits | Free — the count filters on `cached = false` |
| `pending` | Counts as charged, so a failed attempt still costs. Reserving before calling the provider is what stops retries from being free |
| Concurrency | `pg_advisory_xact_lock(hashtext(user_id))` per user |
| Access | `execute` revoked from `public`, `anon` and `authenticated`; granted only to `service_role` |
| Reset | UTC midnight, which is 19:00 in Lima |

Two consequences worth carrying forward:

- The quota is per Supabase user, and anonymous users are free to create. Clearing app data yields a new uuid and a fresh 50, so the limit is effectively per install, not per person. App Check stops an arbitrary `curl`; it does not stop someone holding the phone. Section 2's fallback narrows this for linked users only: their allowance follows the Google account across installs.
- The 19:00 local reset is a UX wart. A user who spends the allowance at midday gets it back in the evening, not at their own midnight.

## 4. Open items

| Priority | Item | Next action |
|---|---|---|
| Done | `signInWithIdToken` fallback | Shipped with `AccountLinkResult`, the `:data` mapping, the inert row and tests. Device check 2026-09-09: anonymous `4233c82a` stayed anonymous, `88b8af3b` got the sign-in one second later |
| Done | Account copy | `settings_google_account_not_linked` now says the allowance is tied to the install |
| Done | `app/google-services.json` on disk | Re-downloaded 2026-09-09 after the fingerprint fix: carries `938a7ce1…`, no bogus hash. `GOOGLE_SERVICES_JSON` secret refreshed from it |
| Done | Swallowed errors | `GoogleCredentialClient` now logs the `NoCredentialException` message, which is where Play Services reports the real reason (on the emulator it was a 3 s Credential Manager timeout, not a missing account) |
| Done | Leftover auth users | Five anonymous users and their 10 test `generation_events` deleted 2026-09-09 through the Management API SQL endpoint. Only `88b8af3b` and `04804872` remain |
| Later | Quota reset hour | UTC midnight lands at 19:00 Lima. Needs a per-user timezone or a rolling window; not worth it before there are users |
