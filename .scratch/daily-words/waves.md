# Daily words: wave plan

Spec: `.scratch/daily-words/spec.md`. Tickets: `.scratch/daily-words/issues/`.

| Wave | Tickets | Why together or apart |
| --- | --- | --- |
| 1 | 01 | The migration ships alone (row 4) |
| 2 | 02, 03 | 02 changes only `supabase/functions/` and 03 only Kotlin. 03 needs 01's schema. The deployed backend strips unknown keys, so 03 does not wait for 02's deploy |
| 3 | 04 | Needs 03's `PrepareDailyWordsUseCase`. Touches `di/NewModule.kt`, `navigation/` and `notifications/` |
| 4 | 05 | Needs 04's settings model and scheduler. Shares `di/NewModule.kt` with 04 |

The owner deploys `suggest-words` by hand after 02 merges (`supabase functions deploy suggest-words`, after a local Supabase run). No CI job deploys functions.
