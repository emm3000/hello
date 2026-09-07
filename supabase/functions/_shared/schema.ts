import { z } from "npm:zod@^4";

export const SCHEMA_VERSION: number = 1;

const NOTE_TYPES = [
  "word",
  "phrase",
  "phrasal_verb",
  "idiom",
  "sentence_pattern",
] as const;

const PARTS_OF_SPEECH = [
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
] as const;

const REGISTERS = ["casual", "neutral", "formal"] as const;

const LEVEL_BANDS = ["A1_A2", "B1_B2", "C1_PLUS"] as const;

const DOMAINS = [
  "daily_life",
  "travel",
  "social",
  "work",
  "study",
  "media",
  "mixed",
] as const;

const CARD_TYPES = ["recognition", "production", "cloze", "form"] as const;

const EVALUATION_MODES = [
  "exact",
  "flexible_text",
  "manual_self_check",
] as const;

const QUALITY_CHECK_CODES = [
  "single_meaning",
  "natural_example",
  "non_ambiguous_answers",
  "clear_card_focus",
  "note_card_alignment",
] as const;

const INPUT_TYPES = [
  "Word",
  "Phrase",
  "Sentence",
  "CommunicativeGoal",
] as const;

const LEARNING_GOALS = ["RecognitionOnly", "ProductionOnly", "Both"] as const;

const REGISTER_PREFERENCES = ["Casual", "Neutral", "Formal"] as const;

const LEARNING_DOMAINS = [
  "DailyLife",
  "Travel",
  "Social",
  "Work",
  "Study",
  "Media",
  "Mixed",
] as const;

const studyCardSchema = z.object({
  card_id: z.string(),
  card_type: z.enum(CARD_TYPES),
  prompt: z.string(),
  expected_answer: z.string(),
  evaluation_mode: z.enum(EVALUATION_MODES),
  is_active: z.boolean().nullish(),
  accepted_answers: z.array(z.string()).nullish(),
  hint: z.string().nullish(),
  explanation: z.string().nullish(),
  source_field: z.string().nullish(),
});

const qualityCheckSchema = z.object({
  code: z.enum(QUALITY_CHECK_CODES),
  passed: z.boolean(),
  message: z.string(),
});

const learningNoteSchema = z.object({
  note_id: z.string(),
  note_type: z.enum(NOTE_TYPES),
  expression: z.string(),
  intended_meaning_es: z.string(),
  simple_definition_en: z.string(),
  part_of_speech: z.enum(PARTS_OF_SPEECH),
  register: z.enum(REGISTERS),
  level_band: z.enum(LEVEL_BANDS),
  domain: z.enum(DOMAINS),
  why_useful: z.string(),
  example_sentence: z.string(),
  example_translation: z.string(),
  cards: z.array(studyCardSchema),
  quality_checks: z.array(qualityCheckSchema),
  lemma: z.string().nullish(),
  ipa: z.string().nullish(),
  usage_pattern: z.string().nullish(),
  irregular_forms: z.array(z.string()).nullish(),
  collocations: z.array(z.string()).nullish(),
  common_mistake: z.string().nullish(),
  confusable_with: z.array(z.string()).nullish(),
  cloze_sentence: z.string().nullish(),
  source_context: z.string().nullish(),
  warnings: z.array(z.string()).nullish(),
});

const generationErrorSchema = z.object({
  input: z.string().nullish(),
  message: z.string(),
});

export const learningNoteResponseSchema = z.object({
  success: z.boolean(),
  data: learningNoteSchema.nullish(),
  error: generationErrorSchema.nullish(),
});

export type LearningNoteResponse = z.infer<typeof learningNoteResponseSchema>;

export function withoutNulls<T>(value: T): T {
  if (Array.isArray(value)) {
    return value.map((item: unknown): unknown => withoutNulls(item)) as T;
  }
  if (value !== null && typeof value === "object") {
    const cleaned: Record<string, unknown> = {};
    for (
      const [key, item] of Object.entries(value as Record<string, unknown>)
    ) {
      if (item === null) {
        continue;
      }
      cleaned[key] = withoutNulls(item);
    }
    return cleaned as T;
  }
  return value;
}

const strictStudyCardSchema = z.strictObject({
  card_id: z.string(),
  card_type: z.enum(CARD_TYPES),
  prompt: z.string(),
  expected_answer: z.string(),
  evaluation_mode: z.enum(EVALUATION_MODES),
  is_active: z.boolean(),
  accepted_answers: z.array(z.string()),
  hint: z.string(),
  explanation: z.string(),
  source_field: z.string(),
});

const strictQualityCheckSchema = z.strictObject({
  code: z.enum(QUALITY_CHECK_CODES),
  passed: z.boolean(),
  message: z.string(),
});

const strictLearningNoteSchema = z.strictObject({
  note_id: z.string(),
  note_type: z.enum(NOTE_TYPES),
  expression: z.string(),
  intended_meaning_es: z.string(),
  simple_definition_en: z.string(),
  part_of_speech: z.enum(PARTS_OF_SPEECH),
  register: z.enum(REGISTERS),
  level_band: z.enum(LEVEL_BANDS),
  domain: z.enum(DOMAINS),
  why_useful: z.string(),
  example_sentence: z.string(),
  example_translation: z.string(),
  cards: z.array(strictStudyCardSchema),
  quality_checks: z.array(strictQualityCheckSchema),
  lemma: z.string(),
  ipa: z.string(),
  usage_pattern: z.string(),
  irregular_forms: z.array(z.string()),
  collocations: z.array(z.string()),
  common_mistake: z.string(),
  confusable_with: z.array(z.string()),
  cloze_sentence: z.string(),
  source_context: z.string(),
  warnings: z.array(z.string()),
});

const strictLearningNoteResponseSchema = z.strictObject({
  success: z.boolean(),
  data: z.union([z.null(), strictLearningNoteSchema]),
  error: z.union([
    z.null(),
    z.strictObject({ input: z.string(), message: z.string() }),
  ]),
});

const strictWordSuggestionSchema = z.strictObject({
  situation: z.string(),
  words: z.array(
    z.strictObject({ word: z.string(), translation: z.string() }),
  ),
});

function toProviderJsonSchema(schema: z.ZodType): Record<string, unknown> {
  const generated: Record<string, unknown> = z.toJSONSchema(schema, {
    io: "output",
    reused: "inline",
  }) as Record<string, unknown>;
  delete generated["$schema"];
  return generated;
}

export const learningNoteJsonSchema: Record<string, unknown> =
  toProviderJsonSchema(strictLearningNoteResponseSchema);

export const wordSuggestionSchema = z.object({
  situation: z.string().min(1),
  words: z.array(
    z.object({ word: z.string().min(1), translation: z.string().min(1) }),
  ).min(1),
});

export type WordSuggestionResponse = z.infer<typeof wordSuggestionSchema>;

export const wordSuggestionJsonSchema: Record<string, unknown> =
  toProviderJsonSchema(strictWordSuggestionSchema);

const collapsedText = z.string().transform((value: string): string =>
  value.trim().replace(/\s+/g, " ")
);

const trimmedText = z.string().transform((value: string): string =>
  value.trim()
);

const ISSUE_CODE_PATTERN: RegExp = /^[a-z][a-z0-9_]*$/;

const ISSUE_FIELD_PATTERN: RegExp = /^[A-Za-z0-9_.\[\]]+$/;

export const generateNoteRequestSchema = z.object({
  input_type: z.enum(INPUT_TYPES),
  user_text: collapsedText.pipe(z.string().min(1).max(500)),
  intended_meaning_es: collapsedText.pipe(z.string().max(500)).default(""),
  context_sentence: collapsedText.pipe(z.string().max(1000)).default(""),
  learning_goal: z.enum(LEARNING_GOALS).default("Both"),
  level_band: z.enum(LEVEL_BANDS).default("A1_A2"),
  register: z.enum(REGISTER_PREFERENCES).default("Neutral"),
  domain: z.enum(LEARNING_DOMAINS).default("DailyLife"),
  communicative_intent_id: trimmedText.pipe(z.string().max(64)).default(""),
  previous_issues: z.array(
    z.object({
      code: z.string().max(64).regex(ISSUE_CODE_PATTERN),
      field: z.string().min(1).max(64).regex(ISSUE_FIELD_PATTERN),
    }),
  ).max(12).default([]),
});

export type GenerateNoteRequest = z.infer<typeof generateNoteRequestSchema>;

export const suggestWordsRequestSchema = z.object({
  recent_words: z.array(z.string().min(1).max(64)).max(50),
});

export type SuggestWordsRequest = z.infer<typeof suggestWordsRequestSchema>;
