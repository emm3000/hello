package com.emm.data.flashcard

import com.google.firebase.ai.type.Schema

/**
 * Estructura esperada de la respuesta de Gemini para la generación principal
 * de [com.emm.data.flashcard.iadto.GeneratedLearningNoteDto]. Restringe enums y
 * marca como opcionales sólo los campos que el DTO también declara con default.
 *
 * Se aplica vía `generationConfig { responseSchema = ... }` en el `GenerativeModel`
 * dedicado a esta llamada (las regeneraciones parciales usan otro modelo sin schema
 * porque tienen shapes distintos).
 */
object LearningNoteResponseSchema {

    val schema: Schema by lazy { buildResponseSchema() }

    private fun buildResponseSchema(): Schema = Schema.obj(
        properties = mapOf(
            "success" to Schema.boolean(),
            "data" to noteSchema(),
            "error" to errorSchema(),
        ),
        optionalProperties = listOf("data", "error"),
    )

    private fun noteSchema(): Schema = Schema.obj(
        properties = mapOf(
            "note_id" to Schema.string(),
            "note_type" to Schema.enumeration(NOTE_TYPES),
            "expression" to Schema.string(),
            "intended_meaning_es" to Schema.string(),
            "simple_definition_en" to Schema.string(),
            "part_of_speech" to Schema.enumeration(PARTS_OF_SPEECH),
            "register" to Schema.enumeration(REGISTERS),
            "level_band" to Schema.enumeration(LEVEL_BANDS),
            "domain" to Schema.enumeration(DOMAINS),
            "why_useful" to Schema.string(),
            "example_sentence" to Schema.string(),
            "example_translation" to Schema.string(),
            "cards" to Schema.array(items = studyCardSchema()),
            "quality_checks" to Schema.array(items = qualityCheckSchema()),
            "lemma" to Schema.string(),
            "ipa" to Schema.string(),
            "usage_pattern" to Schema.string(),
            "irregular_forms" to Schema.array(items = Schema.string()),
            "collocations" to Schema.array(items = Schema.string()),
            "common_mistake" to Schema.string(),
            "confusable_with" to Schema.array(items = Schema.string()),
            "cloze_sentence" to Schema.string(),
            "source_context" to Schema.string(),
            "warnings" to Schema.array(items = Schema.string()),
        ),
        optionalProperties = listOf(
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
        ),
    )

    private fun studyCardSchema(): Schema = Schema.obj(
        properties = mapOf(
            "card_id" to Schema.string(),
            "card_type" to Schema.enumeration(CARD_TYPES),
            "prompt" to Schema.string(),
            "expected_answer" to Schema.string(),
            "evaluation_mode" to Schema.enumeration(EVALUATION_MODES),
            "is_active" to Schema.boolean(),
            "accepted_answers" to Schema.array(items = Schema.string()),
            "hint" to Schema.string(),
            "explanation" to Schema.string(),
            "source_field" to Schema.string(),
        ),
        optionalProperties = listOf(
            "is_active",
            "accepted_answers",
            "hint",
            "explanation",
            "source_field",
        ),
    )

    private fun qualityCheckSchema(): Schema = Schema.obj(
        properties = mapOf(
            "code" to Schema.enumeration(QUALITY_CHECK_CODES),
            "passed" to Schema.boolean(),
            "message" to Schema.string(),
        ),
    )

    private fun errorSchema(): Schema = Schema.obj(
        properties = mapOf(
            "input" to Schema.string(),
            "message" to Schema.string(),
        ),
        optionalProperties = listOf("input"),
    )

    private val NOTE_TYPES = listOf(
        "word",
        "phrase",
        "phrasal_verb",
        "idiom",
        "sentence_pattern",
    )

    private val PARTS_OF_SPEECH = listOf(
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
    )

    private val REGISTERS = listOf("casual", "neutral", "formal")

    private val LEVEL_BANDS = listOf("A1_A2", "B1_B2", "C1_PLUS")

    private val DOMAINS = listOf(
        "daily_life",
        "travel",
        "social",
        "work",
        "study",
        "media",
        "mixed",
    )

    private val CARD_TYPES = listOf("recognition", "production", "cloze", "form")

    private val EVALUATION_MODES = listOf("exact", "flexible_text", "manual_self_check")

    private val QUALITY_CHECK_CODES = listOf(
        "single_meaning",
        "natural_example",
        "example_supports_meaning",
        "non_ambiguous_answers",
        "required_fields_present",
        "clear_card_focus",
        "note_card_alignment",
    )
}
