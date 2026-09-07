import { assertEquals, assertThrows } from "jsr:@std/assert@^1";
import {
  generateNoteRequestSchema,
  learningNoteJsonSchema,
  type LearningNoteResponse,
  learningNoteResponseSchema,
  suggestWordsRequestSchema,
  withoutNulls,
  wordSuggestionSchema,
} from "./schema.ts";

const PROVEN_PROVIDER_SCHEMA: Record<string, unknown> = {
  "type": "object",
  "additionalProperties": false,
  "required": [
    "success",
    "data",
    "error",
  ],
  "properties": {
    "success": {
      "type": "boolean",
    },
    "data": {
      "anyOf": [
        {
          "type": "null",
        },
        {
          "type": "object",
          "additionalProperties": false,
          "required": [
            "note_id",
            "note_type",
            "expression",
            "intended_meaning_es",
            "simple_definition_en",
            "part_of_speech",
            "register",
            "level_band",
            "domain",
            "why_useful",
            "example_sentence",
            "example_translation",
            "cards",
            "quality_checks",
            "lemma",
            "ipa",
            "usage_pattern",
            "irregular_forms",
            "collocations",
            "common_mistake",
            "confusable_with",
            "cloze_sentence",
            "source_context",
            "warnings",
          ],
          "properties": {
            "note_id": {
              "type": "string",
            },
            "note_type": {
              "type": "string",
              "enum": [
                "word",
                "phrase",
                "phrasal_verb",
                "idiom",
                "sentence_pattern",
              ],
            },
            "expression": {
              "type": "string",
            },
            "intended_meaning_es": {
              "type": "string",
            },
            "simple_definition_en": {
              "type": "string",
            },
            "part_of_speech": {
              "type": "string",
              "enum": [
                "noun",
                "verb",
                "adjective",
                "adverb",
                "preposition",
                "conjunction",
                "interjection",
                "phrasal_verb",
                "idiom",
                "chunk",
                "other",
              ],
            },
            "register": {
              "type": "string",
              "enum": [
                "casual",
                "neutral",
                "formal",
              ],
            },
            "level_band": {
              "type": "string",
              "enum": [
                "A1_A2",
                "B1_B2",
                "C1_PLUS",
              ],
            },
            "domain": {
              "type": "string",
              "enum": [
                "daily_life",
                "travel",
                "social",
                "work",
                "study",
                "media",
                "mixed",
              ],
            },
            "why_useful": {
              "type": "string",
            },
            "example_sentence": {
              "type": "string",
            },
            "example_translation": {
              "type": "string",
            },
            "cards": {
              "type": "array",
              "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "card_id",
                  "card_type",
                  "prompt",
                  "expected_answer",
                  "evaluation_mode",
                  "is_active",
                  "accepted_answers",
                  "hint",
                  "explanation",
                  "source_field",
                ],
                "properties": {
                  "card_id": {
                    "type": "string",
                  },
                  "card_type": {
                    "type": "string",
                    "enum": [
                      "recognition",
                      "production",
                      "cloze",
                      "form",
                    ],
                  },
                  "prompt": {
                    "type": "string",
                  },
                  "expected_answer": {
                    "type": "string",
                  },
                  "evaluation_mode": {
                    "type": "string",
                    "enum": [
                      "exact",
                      "flexible_text",
                      "manual_self_check",
                    ],
                  },
                  "is_active": {
                    "type": "boolean",
                  },
                  "accepted_answers": {
                    "type": "array",
                    "items": {
                      "type": "string",
                    },
                  },
                  "hint": {
                    "type": "string",
                  },
                  "explanation": {
                    "type": "string",
                  },
                  "source_field": {
                    "type": "string",
                  },
                },
              },
            },
            "quality_checks": {
              "type": "array",
              "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                  "code",
                  "passed",
                  "message",
                ],
                "properties": {
                  "code": {
                    "type": "string",
                    "enum": [
                      "single_meaning",
                      "natural_example",
                      "non_ambiguous_answers",
                      "clear_card_focus",
                      "note_card_alignment",
                    ],
                  },
                  "passed": {
                    "type": "boolean",
                  },
                  "message": {
                    "type": "string",
                  },
                },
              },
            },
            "lemma": {
              "type": "string",
            },
            "ipa": {
              "type": "string",
            },
            "usage_pattern": {
              "type": "string",
            },
            "irregular_forms": {
              "type": "array",
              "items": {
                "type": "string",
              },
            },
            "collocations": {
              "type": "array",
              "items": {
                "type": "string",
              },
            },
            "common_mistake": {
              "type": "string",
            },
            "confusable_with": {
              "type": "array",
              "items": {
                "type": "string",
              },
            },
            "cloze_sentence": {
              "type": "string",
            },
            "source_context": {
              "type": "string",
            },
            "warnings": {
              "type": "array",
              "items": {
                "type": "string",
              },
            },
          },
        },
      ],
    },
    "error": {
      "anyOf": [
        {
          "type": "null",
        },
        {
          "type": "object",
          "additionalProperties": false,
          "required": [
            "input",
            "message",
          ],
          "properties": {
            "input": {
              "type": "string",
            },
            "message": {
              "type": "string",
            },
          },
        },
      ],
    },
  },
};

const goldNote: Record<string, unknown> = {
  "note_id": "note_dont_make_a_big_deal",
  "note_type": "phrase",
  "expression": "don't make a big deal out of it",
  "intended_meaning_es": "no exageres / no le des tanta importancia",
  "simple_definition_en":
    "tell someone to stop treating a small thing as if it were serious or dramatic.",
  "part_of_speech": "chunk",
  "register": "casual",
  "level_band": "B1_B2",
  "domain": "social",
  "why_useful":
    "lets you defuse drama or push back when someone overreacts to a minor situation — very high frequency in casual conversation.",
  "example_sentence":
    "It was just a small mistake — don't make a big deal out of it.",
  "example_translation": "Fue solo un error pequeño, no exageres.",
  "usage_pattern": "don't make a big deal out of + [noun / it / what + clause]",
  "common_mistake":
    "saying 'don't do a big deal' — the verb is 'make', not 'do'.",
  "source_context":
    "user wants to tell someone to stop dramatizing a small situation",
  "cards": [
    {
      "card_id": "c1",
      "card_type": "recognition",
      "prompt":
        "What does 'don't make a big deal out of it' mean in casual speech?",
      "expected_answer": "no exageres / no le des tanta importancia",
      "evaluation_mode": "flexible_text",
      "is_active": true,
      "source_field": "expression",
    },
    {
      "card_id": "c2",
      "card_type": "production",
      "prompt":
        "Casual English phrase to tell someone 'no exageres' (uses the verb 'make' + 'out of it').",
      "expected_answer": "don't make a big deal out of it",
      "evaluation_mode": "exact",
      "is_active": true,
      "source_field": "expression",
    },
    {
      "card_id": "c3",
      "card_type": "cloze",
      "prompt": "It was just a small mistake — don't ___ out of it.",
      "expected_answer": "make a big deal",
      "evaluation_mode": "exact",
      "is_active": true,
      "source_field": "example_sentence",
    },
  ],
  "quality_checks": [
    {
      "code": "single_meaning",
      "passed": true,
      "message": "One meaning, one target expression.",
    },
    {
      "code": "natural_example",
      "passed": true,
      "message": "Reads as real casual speech.",
    },
    {
      "code": "non_ambiguous_answers",
      "passed": true,
      "message": "Expected answers are short and objective.",
    },
    {
      "code": "clear_card_focus",
      "passed": true,
      "message": "Each card tests one thing.",
    },
    {
      "code": "note_card_alignment",
      "passed": true,
      "message": "All cards target the same expression and meaning.",
    },
  ],
};

function canonical(value: unknown): string {
  if (Array.isArray(value)) {
    return "[" + value.map(canonical).join(",") + "]";
  }
  if (value !== null && typeof value === "object") {
    const entries: [string, unknown][] = Object.entries(
      value as Record<string, unknown>,
    );
    entries.sort((a, b) => a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0);
    return "{" +
      entries.map(([key, item]) => JSON.stringify(key) + ":" + canonical(item))
        .join(",") +
      "}";
  }
  return JSON.stringify(value) ?? "null";
}

Deno.test("the gold example from the prompt parses as a successful note", () => {
  const parsed = learningNoteResponseSchema.parse({
    success: true,
    data: goldNote,
  });
  assertEquals(parsed.success, true);
  assertEquals(parsed.data?.expression, "don't make a big deal out of it");
  assertEquals(parsed.data?.cards.length, 3);
  assertEquals(parsed.data?.quality_checks.length, 5);
});

Deno.test("a refusal without data parses", () => {
  const parsed = learningNoteResponseSchema.parse({
    success: false,
    error: { message: "El texto está vacío." },
  });
  assertEquals(parsed.success, false);
  assertEquals(parsed.data, undefined);
  assertEquals(parsed.error?.message, "El texto está vacío.");
});

Deno.test("a note without cards is rejected", () => {
  const note: Record<string, unknown> = { ...goldNote };
  delete note.cards;
  assertThrows(() =>
    learningNoteResponseSchema.parse({ success: true, data: note })
  );
});

Deno.test("an unknown note_type is rejected", () => {
  const note: Record<string, unknown> = {
    ...goldNote,
    note_type: "collocation",
  };
  assertThrows(() =>
    learningNoteResponseSchema.parse({ success: true, data: note })
  );
});

Deno.test("the exported JSON Schema equals the proven provider schema", () => {
  assertEquals(
    canonical(learningNoteJsonSchema),
    canonical(PROVEN_PROVIDER_SCHEMA),
  );
});

Deno.test("the request schema applies the domain defaults", () => {
  const parsed = generateNoteRequestSchema.parse({
    input_type: "Word",
    user_text: "give up",
  });
  assertEquals(parsed.intended_meaning_es, "");
  assertEquals(parsed.context_sentence, "");
  assertEquals(parsed.learning_goal, "Both");
  assertEquals(parsed.level_band, "A1_A2");
  assertEquals(parsed.register, "Neutral");
  assertEquals(parsed.domain, "DailyLife");
  assertEquals(parsed.communicative_intent_id, "");
  assertEquals(parsed.previous_issues, []);
});

Deno.test("the request schema trims and collapses free text", () => {
  const parsed = generateNoteRequestSchema.parse({
    input_type: "CommunicativeGoal",
    user_text: "  give   up  ",
    intended_meaning_es: "\n rendirse   ya \n",
    context_sentence: "  he   gave up  ",
    communicative_intent_id: "  intent-1  ",
    previous_issues: [{
      code: "missing_usage_pattern",
      field: "usage_pattern",
    }],
  });
  assertEquals(parsed.user_text, "give up");
  assertEquals(parsed.intended_meaning_es, "rendirse ya");
  assertEquals(parsed.context_sentence, "he gave up");
  assertEquals(parsed.communicative_intent_id, "intent-1");
  assertEquals(parsed.previous_issues.length, 1);
});

Deno.test("an empty user_text is rejected", () => {
  assertThrows(() =>
    generateNoteRequestSchema.parse({ input_type: "Word", user_text: "" })
  );
});

Deno.test("the suggestion schema needs a situation and at least one word", () => {
  const parsed = wordSuggestionSchema.parse({
    situation: "Ordering coffee",
    words: [{ word: "refill", translation: "recarga" }],
  });
  assertEquals(parsed.words.length, 1);
  assertThrows(() =>
    wordSuggestionSchema.parse({ situation: "Ordering coffee", words: [] })
  );
});

Deno.test("the suggest-words request accepts recent words", () => {
  const parsed = suggestWordsRequestSchema.parse({ recent_words: ["give up"] });
  assertEquals(parsed.recent_words, ["give up"]);
  assertThrows(() =>
    suggestWordsRequestSchema.parse({
      recent_words: new Array(51).fill("word"),
    })
  );
});

Deno.test("a successful envelope with an explicit null error parses", () => {
  const parsed = learningNoteResponseSchema.parse({
    success: true,
    data: goldNote,
    error: null,
  });
  assertEquals(parsed.success, true);
  assertEquals(parsed.error, null);
  assertEquals(parsed.data?.expression, "don't make a big deal out of it");
});

Deno.test("null optional note and card fields parse", () => {
  const cards: Record<string, unknown>[] =
    (goldNote.cards as Record<string, unknown>[]).map(
      (card: Record<string, unknown>, index: number): Record<string, unknown> =>
        index === 0 ? { ...card, hint: null } : card,
    );
  const note: Record<string, unknown> = {
    ...goldNote,
    lemma: null,
    cloze_sentence: null,
    warnings: null,
    cards,
  };
  const parsed = learningNoteResponseSchema.parse({
    success: true,
    data: note,
    error: null,
  });
  assertEquals(parsed.data?.lemma, null);
  assertEquals(parsed.data?.cloze_sentence, null);
  assertEquals(parsed.data?.warnings, null);
  assertEquals(parsed.data?.cards[0].hint, null);
});

Deno.test("withoutNulls drops null keys and keeps every falsy value", () => {
  const raw: Record<string, unknown> = {
    success: true,
    data: {
      lemma: null,
      cloze_sentence: null,
      warnings: null,
      collocations: [],
      source_context: "",
      cards: [
        {
          card_id: "c1",
          is_active: false,
          accepted_answers: [],
          hint: null,
          explanation: "",
        },
      ],
    },
  };
  const cleaned: Record<string, unknown> = withoutNulls(raw);
  assertEquals(cleaned, {
    success: true,
    data: {
      collocations: [],
      source_context: "",
      cards: [
        {
          card_id: "c1",
          is_active: false,
          accepted_answers: [],
          explanation: "",
        },
      ],
    },
  });
});

Deno.test("a refusal with a null data and a null input is parsed and cleaned", () => {
  const parsed = learningNoteResponseSchema.parse({
    success: false,
    data: null,
    error: { input: null, message: "El texto está vacío." },
  });
  assertEquals(parsed.data, null);
  assertEquals(parsed.error?.input, null);
  const cleaned: LearningNoteResponse = withoutNulls(parsed);
  assertEquals(cleaned, {
    success: false,
    error: { message: "El texto está vacío." },
  });
});
