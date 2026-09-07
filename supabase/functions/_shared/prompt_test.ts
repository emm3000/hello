import { assertEquals, assertStringIncludes } from "jsr:@std/assert@^1";
import {
  buildLearningNotePrompt,
  buildWordSuggestionPrompt,
  PROMPT_VERSION,
  withSchemaAppendix,
} from "./prompt.ts";
import { generateNoteRequestSchema } from "./schema.ts";

const INPUT_DATA_BLOCK = `Input data:
- input_type: "Word"
- user_text: "give up"
- intended_meaning_es: ""
- context_sentence: ""
- learning_goal: "Both"
- level_band: "A1_A2"
- register: "Neutral"
- domain: "DailyLife"
- communicative_intent_id: ""`;

const FEEDBACK_SECTION =
  `Your previous answer for this exact input was rejected by these deterministic checks:
- missing_usage_pattern (field: usage_pattern)
- ambiguous_answer (field: cards)
Fix every listed field and return the complete JSON again.`;

Deno.test("the prompt version is one", () => {
  assertEquals(PROMPT_VERSION, 1);
});

Deno.test("the learning note prompt carries the input data block", () => {
  const prompt: string = buildLearningNotePrompt(
    generateNoteRequestSchema.parse({
      input_type: "Word",
      user_text: "give up",
    }),
  );
  assertStringIncludes(prompt, INPUT_DATA_BLOCK);
  assertEquals(
    prompt.startsWith(
      "You are a bilingual English-learning assistant for native Spanish speakers",
    ),
    true,
  );
  assertEquals(
    prompt.endsWith(
      "Do not include markdown or any text outside the JSON.",
    ),
    true,
  );
});

Deno.test("the learning note prompt omits the feedback section without issues", () => {
  const prompt: string = buildLearningNotePrompt(
    generateNoteRequestSchema.parse({
      input_type: "Word",
      user_text: "give up",
    }),
  );
  assertEquals(
    prompt.includes("Your previous answer for this exact input"),
    false,
  );
});

Deno.test("the learning note prompt appends every previous issue", () => {
  const prompt: string = buildLearningNotePrompt(
    generateNoteRequestSchema.parse({
      input_type: "Word",
      user_text: "give up",
      previous_issues: [
        { code: "missing_usage_pattern", field: "usage_pattern" },
        { code: "ambiguous_answer", field: "cards" },
      ],
    }),
  );
  assertStringIncludes(prompt, "\n\n" + FEEDBACK_SECTION);
  assertEquals(prompt.endsWith(FEEDBACK_SECTION), true);
});

Deno.test("the suggestion prompt describes a beginner without recent words", () => {
  const prompt: string = buildWordSuggestionPrompt([]);
  assertStringIncludes(
    prompt,
    "The learner is a total beginner with no recorded vocabulary yet. Pick an A2-level everyday situation.",
  );
  assertEquals(
    prompt.startsWith(
      "You are an English tutor for a Spanish-speaking learner.\n",
    ),
    true,
  );
});

Deno.test("the suggestion prompt lists the recent words", () => {
  const prompt: string = buildWordSuggestionPrompt(["give up", "afford"]);
  assertStringIncludes(
    prompt,
    "The learner has recently studied these English words or expressions: give up, afford. Infer the learner's approximate level from this list.",
  );
  assertEquals(
    prompt.endsWith(
      `{"situation": "<one short English sentence>", "words": [{"word": "...", "translation": "..."}]}`,
    ),
    true,
  );
});

Deno.test("the schema appendix ends with the schema json", () => {
  const jsonSchema: Record<string, unknown> = { type: "object" };
  const appended: string = withSchemaAppendix("PROMPT", jsonSchema);
  assertEquals(
    appended,
    "PROMPT\n\nRespond with a single JSON object that validates against this JSON Schema, with no markdown and no text outside the JSON:\n" +
      JSON.stringify(jsonSchema),
  );
});
