# 02: Backend: suggest-words returns 3 words and 3 phrases and honours an exclusion list

Status: ready-for-agent
Device: none
Blocked by: None
**Branch:** feat/02-suggest-words-mix-and-exclusions

Spec decisions 4a, 5 and 6 in `.scratch/daily-words/spec.md`. Gate row 4 (`supabase/functions/`): write a test per failure branch. Files: `supabase/functions/_shared/prompt.ts` `buildWordSuggestionPrompt`, `_shared/schema.ts` `suggestWordsRequestSchema`, `suggest-words/index.ts`, and the `*_test.ts` files beside them.
The contract shared with ticket 03 adds two optional request fields, both defaulting to `[]`: `excluded_situations` (at most 30 strings of 1 to 200 chars) and `excluded_words` (at most 120 strings of 1 to 120 chars). The response shape `{"situation", "words": [{"word", "translation"}]}` does not change, so shipped clients keep parsing it. Credits and daily limits stay as they are.
Deploy: no workflow deploys functions (`.github/workflows/` holds only `pr-checks`, `supabase-heartbeat` and `uploadApk`). After merge, the owner exercises the function on local Supabase and then runs `supabase functions deploy suggest-words` by hand against the single hosted project. The peer does not deploy.

## Acceptance
- [ ] `rg -n 'exactly 6 English words' supabase/functions/_shared/prompt.ts` prints nothing
- [ ] A `prompt_test.ts` case asserts the prompt asks for exactly 3 single words and exactly 3 full natural phrases a native speaker would say in the situation, each with a Spanish translation
- [ ] A `prompt_test.ts` case: with exclusions, the prompt lists the excluded situations and words and forbids reusing them. Without exclusions, no exclusion text appears
- [ ] A `schema_test.ts` case: a body with only `recent_words` parses and both exclusion fields come back as `[]`
- [ ] Schema or `index_test.ts` cases: 31 situations, 121 words or an empty string are each rejected with the existing invalid-request response
- [ ] An `index_test.ts` case: the provider receives a prompt that contains a sent exclusion
- [ ] `deno task --cwd supabase/functions check` passes
- [ ] `scripts/check` green
