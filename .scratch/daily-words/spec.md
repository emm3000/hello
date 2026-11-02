# Daily words

Agreed with the owner on 2026-11-02.

## Problem

New vocabulary only appears when the learner opens `Suggest` from `Hoy` (zero cards due) or from the end of a study session. Nothing brings fresh words to the learner. The learner wants a notification at a time they choose. It should offer new everyday words and phrases they can turn into cards, and it should never repeat what it already showed.

## What the learner sees

- Settings has a "New words" section, separate from the study reminder. It has an on/off switch, a time, and the weekdays it fires on. Defaults: on, 09:00, every day.
- At that time on a selected day, a notification names the situation, e.g. "Today: asking for the check at a restaurant · 6 new words and phrases". Its copy is in English and Spanish, like the rest of the app.
- Tapping it opens `Suggest` showing that batch. The batch is 3 single words and 3 full phrases a native speaker would really say in that situation, each with its Spanish translation. Choosing items and adding them works as it does today.
- The study reminder (`DueCardsReminderWorker`, `docs/NOTIFICATIONS_PLAN.md`) keeps its behaviour, its own toggle and its own time.

## Decisions

1. **Device-side scheduling.** A WorkManager worker does the scheduling, like the study reminder. There is no FCM, no server cron and no upload of the learner's vocabulary. The vocabulary stays in `HelloDb` (`LOCAL_FIRST.md`). Rejected: server push. The backend cannot see the learner's words, and it would need FCM tokens, per-user time zones and a Data safety change.
2. **Pregenerate, then notify.** The worker fetches the batch before posting, so the notification can name the situation and the screen opens offline. If the fetch fails or the device is offline, the worker posts nothing that day. It also posts nothing when a freshly fetched batch has no usable items.
3. **No spend on an unopened batch.** The daily batch is the same cached batch `Suggest` already uses: the singleton in `WordSuggestionBatch`, `docs/SUGGEST_CURRENT.md`. The batch records when the learner first saw it loaded on `Suggest`. If the current batch is unseen and still has usable items, the worker re-posts the notification for it without calling the backend. It fetches a new batch when the current one was seen, is absent, or has no usable items left, so the notification never goes silent for good.
4. **Never repeat, two layers.**
   - (a) A local history keeps every situation and item shown in the last 60 days. The most recent entries, capped to bound the prompt, go to `suggest-words` as an exclusion list.
   - (b) After the response arrives, the device drops any item already in that history or already a card. This extends `SuggestedWordFilter`.
   - The history is written when a batch is stored, not when it is seen. Entries older than 60 days are pruned.
5. **Words and phrases mix.** The `suggest-words` prompt asks for exactly 3 single words and 3 full natural phrases. The new exclusion field is optional, so older app versions that do not send it keep working. The mix also applies when `Suggest` is opened from `Hoy` or from the end of a session.
6. **Quota.** A daily fetch is a normal `suggest-words` call. It goes through the existing credits and daily usage limits, with no bypass.

## Out of scope

- Server push, multiple notifications per day, per-deck targeting.
- Generating full cards in advance. Cards are still generated through `generate-note` when the learner adds an item.

## Pointers

- `docs/NOTIFICATIONS_PLAN.md`, `docs/SUGGEST_CURRENT.md`, `docs/SETTINGS_CURRENT.md`, `LOCAL_FIRST.md`
- `.claude/rules/sqldelight.md` for the history table and the seen marker
- `supabase/functions/_shared/prompt.ts` `buildWordSuggestionPrompt`, `supabase/functions/suggest-words/`
- `app/src/main/kotlin/com/emm/hello/notifications/`, `navigation/LaunchDestination`
- `domain/src/main/kotlin/com/emm/domain/suggestion/`, `data/src/main/kotlin/com/emm/data/suggestion/`
