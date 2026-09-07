# AI Backend Plan

| Field | Value |
|---|---|
| Status | Proposed, 2026-09-07 |
| Progress | Phases 1, 2 and 3 implemented on 2026-09-07 (local stack); credits and account linking pending. |
| Role | Plan for moving every AI call behind one Supabase Edge Function |
| Source of Truth | No. `*_CURRENT.md` and the code win. Becomes history once shipped. |
| Read this when | You touch AI generation, quotas, guest identity or the `supabase/` directory |

Every AI call moves from Firebase AI Logic inside the app to one Supabase Edge Function that owns the prompt, a provider chain, a shared note cache and a daily allowance per guest user. The app keeps the JSON contract and the `:domain` validation it has today, so the client change is a transport swap in `:data`.

## Why this exists

| Problem measured on 2026-09-06 and 2026-09-07 | Consequence |
|---|---|
| `gemini-2.5-flash-lite` free tier gave 20 requests per day to project `hello-e73d7` | A handful of captures exhausts the day |
| `gemini-2.5-flash-lite` returns `404 no longer available to new users` on a fresh key | The current model is on its way out |
| A direct Gemini or OpenRouter key inside the APK is extractable | Any provider other than Firebase AI Logic needs a server |
| Cache and credits are only enforceable on a server that sees every request | A client-side fallback would bypass both |

## Decisions

| # | Decision | Why |
|---|---|---|
| 1 | One path: app to Edge Function to provider chain. Firebase AI Logic is removed. | Two paths make the cache blind and the allowance unenforceable. |
| 2 | Firebase stays for App Check, Crashlytics and Analytics only. | Already wired and verified on device. |
| 3 | Prompt and JSON schema live on the server, versioned by `prompt_version` and `schema_version`. | Prompt improvements ship without an APK release. Version bumps invalidate the cache by themselves. |
| 4 | The response envelope `success / data / error` stays byte-compatible with today. | `GeneratedLearningNoteResponseParser` and all `:domain` policies stay untouched. |
| 5 | Guest identity is a Supabase anonymous user, created lazily on first AI use, never at startup. | Startup stays local. Credits need a stable subject. `linkIdentity` upgrades the same user later. |
| 6 | The Firebase App Check token is verified inside the function as a second factor. | Anonymous sign-ups can be scripted; consuming allowance cannot. |
| 7 | Credits are charged only on provider success. Cache hits, refusals and provider failures are free. | No refund logic. Users never pay for a typo. |
| 8 | Daily allowance instead of a lifetime count. | Two lifetime generations cannot demonstrate a spaced-repetition app. Free provider quotas reset daily too. |
| 9 | Providers are OpenAI-compatible configs in an ordered array. No plugin system. | Gemini and OpenRouter share the wire format, so a provider is base URL, key, model and output mode. |
| 10 | The legacy `supabase/` directory is deleted and re-initialised. | Its migrations describe the sync stack removed in `0c512da`; none of it applies. |

## Architecture

### Request flow for a captured word

1. `FlashcardEnrichmentWorker` asks `SupabaseSessionInitializer.ensureSession()`. First call signs in anonymously; the SDK persists the session.
2. `RemoteFlashcardGenerationRepository` posts the structured input to `generate-note` with two headers: `Authorization: Bearer <session JWT>` and `X-Firebase-AppCheck: <App Check token>`.
3. The Supabase gateway rejects an invalid session before the function runs (`verify_jwt = true`).
4. The function runs, in this order: verify App Check, build the cache key, read the cache, check the allowance, call the provider chain, validate with zod, write the cache, write the event, respond.
5. The app parses `data` with the existing parser. A `:domain` rejection retries once with `previous_issues`; that request skips the cache read and overwrites the entry.

Cache read runs before the allowance check so hits never consume. The allowance check runs before any provider call so an exhausted user costs nothing.

### Components

| Layer | Component | Responsibility |
|---|---|---|
| `supabase/functions/generate-note` | `index.ts` | HTTP handler for learning notes |
| `supabase/functions/suggest-words` | `index.ts` | HTTP handler for the Suggest flow |
| `supabase/functions/_shared` | `appcheck.ts` | Verify the App Check JWT against Firebase JWKS |
| | `providers.ts` | Ordered provider configs, one HTTP client, circuit breaker |
| | `prompt.ts` | Learning-note and suggestion prompts, `PROMPT_VERSION` |
| | `schema.ts` | zod schema, `SCHEMA_VERSION`, JSON Schema export for strict providers |
| | `cache.ts` | Cache key, read, write, negative TTL |
| | `credits.ts` | Event write today; daily allowance check arrives with phase 4 |
| | `provider_state.ts` | Persisted `provider_state` store for the circuit breaker |
| `:data` | `RemoteFlashcardGenerationRepository` | Transport for `generate-note`, HTTP to domain error mapping |
| | `RemoteWordSuggestionRepository` | Transport for `suggest-words` |
| | `SupabaseSessionInitializer` | Lazy anonymous session |
| `:domain` | `GenerationCreditsExhaustedException` | New typed error for `402` |

### Providers

| Order | Provider | Model | Output mode | Timeout | Cooldown on `429` |
|---|---|---|---|---|---|
| 1 | Gemini Developer API, OpenAI endpoint | `gemini-3.1-flash-lite` | `json_schema` | 30 s | until next 00:00 America/Los_Angeles |
| 2 | OpenRouter | `minimax/minimax-m3:free` | `json_object`, schema embedded in the prompt | 45 s | 1 h |

Rules:

- A `429` marks the provider exhausted in the in-memory map of the isolate and in the `provider_state` table, so every isolate and both functions honour the cooldown.
- A `503`, a timeout or a zod failure skips to the next provider for this request only.
- At most two provider attempts per request. Total budget stays under 90 s, inside the 150 s free-plan wall clock.
- Every provider response passes the zod schema before it is cached or returned, whatever the output mode.

### Data model

All three tables are reachable only with the service role. No client policy, no RLS exposure.

```sql
create table note_cache (
  cache_key text primary key,
  response jsonb not null,
  success boolean not null,
  provider text not null,
  model text not null,
  prompt_version int not null,
  schema_version int not null,
  hits int not null default 0,
  expires_at timestamptz,
  created_at timestamptz not null default now()
);

create table generation_events (
  id bigint generated always as identity primary key,
  user_id uuid not null,
  operation text not null,
  cache_key text,
  provider text,
  model text,
  cached boolean not null,
  outcome text not null,
  created_at timestamptz not null default now()
);
create index generation_events_user_day on generation_events (user_id, created_at desc);

create table provider_state (
  provider_id text primary key,
  exhausted_until timestamptz
);
```

`generation_events` is both the credit ledger and the telemetry. When billing arrives, a `credit_grants` table joins it and balance becomes grants minus charged events. Nothing migrates.

### Cache key

`sha256` of the canonical JSON of:

| Field | Normalisation |
|---|---|
| `user_text` | trim, collapse whitespace, lowercase |
| `input_type`, `learning_goal`, `level_band`, `register`, `domain` | as sent |
| `intended_meaning_es`, `context_sentence`, `communicative_intent_id` | trim, collapse whitespace; usually empty |
| `prompt_version`, `schema_version` | server constants |

`previous_issues` is never part of the key. Its presence means regenerate.

Refusals (`success = false`) are cached with `expires_at = now() + 24 h`. Successes never expire.

### Credits

| Rule | Value |
|---|---|
| Allowance | `DAILY_ALLOWANCE` successes per user per UTC day, shared by `generate-note` and `suggest-words` |
| Counted | `generation_events` rows with `outcome = 'success'` and `cached = false` since 00:00 UTC |
| Charged | Only when a provider returned a valid note or suggestion |
| Reported | `meta.credits_remaining` on every response so the UI can show the count |

Concurrent requests from one user may exceed the allowance by one. Accepted.

## API contract

### `POST /functions/v1/generate-note`

Headers:

| Header | Value |
|---|---|
| `Authorization` | `Bearer <Supabase session JWT>` |
| `X-Firebase-AppCheck` | App Check token from `FirebaseAppCheck.getAppCheckToken(false)` |
| `Content-Type` | `application/json` |

Request:

```json
{
  "input_type": "Word",
  "user_text": "give up",
  "intended_meaning_es": "",
  "context_sentence": "",
  "learning_goal": "Both",
  "level_band": "A1_A2",
  "register": "Neutral",
  "domain": "DailyLife",
  "communicative_intent_id": "",
  "previous_issues": []
}
```

Response, `200`:

```json
{
  "success": true,
  "data": { "note_id": "...", "note_type": "phrasal_verb", "...": "unchanged from today" },
  "error": null,
  "meta": { "cached": false, "provider": "gemini", "model": "gemini-3.1-flash-lite", "prompt_version": 1 }
}
```

`meta.credits_remaining` arrives with phase 4.

A refusal is `200` with `success: false` and `error.message` in neutral Latin American Spanish, exactly as today.

Errors:

| HTTP | `error.code` | Domain exception in `:data` | Worker behaviour |
|---|---|---|---|
| `401` | `app_check_rejected` or gateway rejection | `AppCheckRejectedException` (exists) | Not retried |
| `402` | `credits_exhausted` | `GenerationCreditsExhaustedException` (new) | Not retried; failure reason shown on the card row |
| `200` | `success: false` | `AmbiguousGenerationInputException` (exists) | Not retried |
| `503` | `providers_exhausted`, with `retry_after` seconds | `IOException` (`GenerationQuotaExceededException` is deleted, not reused) | Retried by the existing WorkManager backoff |
| `5xx` other | any | `IOException` | Retried by the existing backoff |

### `POST /functions/v1/suggest-words`

Same headers. Request `{ "recent_words": ["..."] }`. Response body is the JSON `WordSuggestionResponseParser` parses today, plus the same `meta` object. No cache in v1.

## App changes

### Removed

| Item | Location |
|---|---|
| `api(libs.firebase.ai)` | `data/build.gradle.kts` |
| `GeminiService`, `LearningNoteResponseSchema`, `Prompt` | `data/.../flashcard/` |
| `WordSuggestionPrompt` | `data/.../suggestion/` |
| `DailyGenerationQuota` and the `GenerationQuota` binding | `:data` and `RepositoryModule.kt` |
| `provideGenericModel`, `provideLearningNoteModel` | `RepositoryModule.kt` |
| `FakeGeminiService` | `app/src/androidTest` |
| Legacy `supabase/` directory, `supabase-*` entries in `libs.versions.toml` re-added only for the modules that use them | repo root, `gradle/` |

### Added

| Item | Notes |
|---|---|
| `supabase-bom`, `auth-kt` in `:data` | Already declared in `libs.versions.toml` at 3.8.0. Transport is plain Ktor (`HttpFunctionsTransport`); `functions-kt` is not used. |
| `SupabaseClient` singleton in DI | Reads `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_PUBLISHABLE_KEY`; debug defaults to `http://127.0.0.1:54321` and the local demo publishable key, overridable via `supabase.url` and `supabase.publishableKey` in `local.properties`; release and staging read `local.properties` only. The emulator reaches it through adb reverse tcp:54321 tcp:54321, run after every emulator boot; on macOS with Docker Desktop, ports published by Docker are not reachable from the emulator via 10.0.2.2. |
| `SupabaseSessionInitializer.ensureSession()` | Called by the worker and by Suggest before the first call |
| `RemoteFlashcardGenerationRepository` | Replaces `DefaultFlashcardGenerationRepository` |
| `RemoteWordSuggestionRepository` | Replaces `GeminiWordSuggestionRepository`; `USE_CANNED_AI` keeps selecting `CannedWordSuggestionRepository` |
| `GenerationCreditsExhaustedException` | `:domain`, carries `resetAt` |
| `GenerationTelemetry` | `GeminiTelemetry` renamed; same Crashlytics sink |
| Client request timeout 100 s | Was 15 s; covers the provider chain's 30 s + 45 s server-side budget |

`EnrichmentRetryPolicy` adds `GenerationCreditsExhaustedException` to its non-retryable set. `LOCAL_FIRST.md` changes one line: generation goes through the Hello backend instead of Firebase AI.

## Configuration

| Where | Setting |
|---|---|
| Supabase Auth | Anonymous sign-ins enabled |
| `supabase/config.toml` | `[functions.generate-note] verify_jwt = true`, same for `suggest-words` |
| Function secrets | `GEMINI_API_KEY`, `OPENROUTER_API_KEY`, `FIREBASE_PROJECT_NUMBER` for local development, set in `supabase/functions/.env` (gitignored; `.env.example` lists the names); `DAILY_ALLOWANCE` is not read yet |
| App `BuildConfig` | `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` per flavour, from `local.properties` like the other secrets |
| GitHub Actions | Daily heartbeat query so the free project is never paused for inactivity |

## Phases

Each phase is one work unit with its falsifier. Nothing ships without it.

| Phase | Scope | Falsifier | Status |
|---|---|---|---|
| 1 Cleanup | Delete `supabase/`, run `supabase init`, drop unused `supabase-*` aliases | `rg -i supabase` returns only the new tree and the Gradle aliases still in use | Done — `rg -i supabase` clean |
| 2 Function, auth, providers | `generate-note` and `suggest-words` without cache or credits; app migrated; Firebase AI removed | A capture on `medium_phone` ends READY through the function. A request without the App Check header returns `401`. With an invalid `GEMINI_API_KEY` the note arrives from MiniMax and `meta.provider` says so. | Done — 28 Deno tests, `401` without App Check, `503 providers_exhausted` with a real App Check token and no keys, JVM tests for the mapper and repositories; device READY verified on 2026-09-07 ("look forward to" captured on Medium_Phone_2 came back from Gemini); with an invalid Gemini key the note arrived from OpenRouter with meta.provider = "openrouter" |
| 3 Cache | `note_cache`, negative TTL, `previous_issues` bypass | The second request for the same word returns `cached: true` in under 200 ms and `generation_events` shows `cached = true` with no provider | Done — 40 Deno tests; migration `20260907135456_ai_backend_cache.sql` applied with `supabase migration up`; device-verified on 2026-09-07: "make up" came from Gemini in 5662 ms, then "Make  Up" (same key after normalisation) returned `cached: true` in 18 ms with no `provider_attempt`, `note_cache.hits = 1`, and its `generation_events` row has `cached = true` and a null provider |
| 4 Credits | `generation_events` count, `402`, UI reason line | Request number `DAILY_ALLOWANCE + 1` returns `402` and the card row shows the reason, like the AI refusal does today | Pending |
| 5 Link account | Google sign-in through `linkIdentity` | `user_id` and event count are identical before and after linking | Pending |
| 6 Billing | Out of scope for this plan | | Pending |

## Risks

| Risk | Mitigation |
|---|---|
| Supabase Free pauses a project after about seven days of low database activity | Event writes are activity; a daily GitHub Actions heartbeat covers idle weeks |
| Free provider endpoints saturate or disappear | Circuit breaker plus second provider; a `503` from both surfaces as `providers_exhausted` with `retry_after` |
| Anonymous account farming by reinstalling | App Check with Play Integrity on release builds; accepted for the MVP |
| Latency: Gemini Flash-Lite 2 to 15 s, MiniMax 17 to 24 s | Enrichment already runs in WorkManager; the user is never blocked |
| OpenRouter unfunded accounts get 50 free requests per day | A one-time 10 USD credit purchase raises it to 1000 per day; decision deferred |

## Open decisions

- `DAILY_ALLOWANCE` value. Proposal: 5.
- Whether Suggest shares the allowance or stays free. Proposal: shares it.
- Whether to fund OpenRouter once for the 1000 per day tier.

## Evidence

Probe of 2026-09-07 with the production prompt and a strict JSON schema, temperature 0:

| Model | Valid outputs | Latency |
|---|---|---|
| `gemini-3.1-flash-lite` | 3 of 3 | 1.7 to 6.7 s |
| `gemini-3.5-flash-lite` | 2 of 2, one `503` | 3.8 to 15 s |
| `gemini-3.7-flash`, `gemini-3.8-flash`, `gemma-4-31b-it` | `503 high demand` on every call | |
| `minimax/minimax-m3:free` via OpenRouter | 3 of 3 | 17 to 24 s |
| `dots-3-note-preview:free` | 5 of 5 with reasoning, copies the gold example without it | 30 to 38 s |
| `nemotron-3-super-120b:free` | empty content 6 of 8 | 54 to 161 s |
