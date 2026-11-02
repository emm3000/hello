# 02: Backend: suggest-words returns 3 words and 3 phrases and honours an exclusion list

Status: done
Device: none
Blocked by: None
**Branch:** feat/02-suggest-words-mix-and-exclusions

Spec decisions 4a, 5 and 6 in `.scratch/daily-words/spec.md`. Gate row 4 (`supabase/functions/`): write a test per failure branch. Files: `supabase/functions/_shared/prompt.ts` `buildWordSuggestionPrompt`, `_shared/schema.ts` `suggestWordsRequestSchema`, `suggest-words/index.ts`, and the `*_test.ts` files beside them.
The contract shared with ticket 03 adds two optional request fields, both defaulting to `[]`: `excluded_situations` (at most 30 strings of 1 to 200 chars) and `excluded_words` (at most 120 strings of 1 to 120 chars). The response shape `{"situation", "words": [{"word", "translation"}]}` does not change, so shipped clients keep parsing it. Credits and daily limits stay as they are.
Deploy: no workflow deploys functions (`.github/workflows/` holds only `pr-checks`, `supabase-heartbeat` and `uploadApk`). After merge, the owner exercises the function on local Supabase and then runs `supabase functions deploy suggest-words` by hand against the single hosted project. The peer does not deploy.

## Acceptance
- [x] `rg -n 'exactly 6 English words' supabase/functions/_shared/prompt.ts` prints nothing
- [x] A `prompt_test.ts` case asserts the prompt asks for exactly 3 single words and exactly 3 full natural phrases a native speaker would say in the situation, each with a Spanish translation
- [x] A `prompt_test.ts` case: with exclusions, the prompt lists the excluded situations and words and forbids reusing them. Without exclusions, no exclusion text appears
- [x] A `schema_test.ts` case: a body with only `recent_words` parses and both exclusion fields come back as `[]`
- [x] Schema or `index_test.ts` cases: 31 situations, 121 words or an empty string are each rejected with the existing invalid-request response
- [x] An `index_test.ts` case: the provider receives a prompt that contains a sent exclusion
- [x] `deno task --cwd supabase/functions check` passes
- [x] `scripts/check` green

## Comments

Evidence on 389e64de:
- `rg -n 'exactly 6 English words' supabase/functions/_shared/prompt.ts` prints nothing (exit 1).
- `prompt_test.ts`: "the suggestion prompt asks for 3 single words and 3 full natural phrases with Spanish translations", "the suggestion prompt lists the excluded situations and words and forbids reusing them", "the suggestion prompt carries only the exclusion line that has entries", "the suggestion prompt carries no exclusion text without exclusions".
- `schema_test.ts`: "a suggest-words body with only recent words defaults both exclusion lists to empty", plus the 200/201-char situation, 120/121-char word, empty word and 30/120 cap cases.
- `index_test.ts`: "the provider receives a prompt carrying the sent exclusions"; "31 excluded situations", "121 excluded words" and "an empty excluded situation" each return 400 `invalid_request` naming the field, before any credit or provider call. The four handler tests failed against the previous schema and prompt and pass now.
- `deno task --cwd supabase/functions check`: exit 0, 137 passed, 0 failed.
- `scripts/check` (pre-commit): gate green.
- `docs/SUGGEST_CURRENT.md` describes the new prompt and request fields.
- Not deployed. The owner exercises `suggest-words` on local Supabase and deploys it by hand after merge.
