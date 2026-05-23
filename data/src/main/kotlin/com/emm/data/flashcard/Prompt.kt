package com.emm.data.flashcard

import com.emm.data.catalog.communicativeIntents

object Prompt {

    @Suppress("LongMethod")
    fun buildLearningNotePrompt(input: com.emm.domain.flashcard.FlashcardGenerationInput): String {
        val communicativeIntent = communicativeIntents
            .firstOrNull { it.id == input.communicativeIntentId }

        return """
        You are a bilingual English-learning assistant for native Spanish speakers
        (neutral Latin American Spanish; avoid Iberian voseo/leísmo).

        Generate exactly one structured learning note for spaced repetition.
        Design it like a high-value Anki/SRS note, not a generic lesson or vocabulary dump.

        Principles (non-negotiable):
        - Minimum information: one meaning, one expression, one communicative purpose per note.
        - High utility first: the most useful next expression for real communication.
        - Retrieval before recognition: each card is one clear recall task.
        - No laundry lists: never group multiple expressions, mini-lessons, or category overviews.
        - Concrete context beats abstract: examples sound like real speech, not textbook.
        - Reduce interference: cards must not be near-duplicates of each other.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"
        - learning_goal: "${input.learningGoal.name}"
        - level_band: "${input.levelBand.name}"
        - register: "${input.register.name}"
        - domain: "${input.domain.name}"
        - communicative_intent_id: "${input.communicativeIntentId}"
        - communicative_intent_label: "${communicativeIntent?.label.orEmpty()}"
        - communicative_intent_description: "${communicativeIntent?.description.orEmpty()}"

        Decision policy:
        - If the input is a broad communicative goal, infer the single highest-value English expression.
        - For A1_A2, prefer high-frequency expressions that unlock real interaction quickly.
        - For B1_B2 and C1_PLUS, prefer expressions with strong communicative payoff, nuance, or common learner confusion.
        - When several options are possible, choose the most frequent, reusable, and retrieval-friendly one.
        - Capture the user's situation briefly in source_context, in your own words.

        Accepting the input:
        - user_text may be Spanish or English. It may be a single word, a short phrase, or a broader communicative goal — all of them are valid.
        - Single Spanish words like "querer", "aprender", "ahorrar" → pick the most common English target (e.g. "want", "learn", "save"). Do NOT reject these as ambiguous.
        - Single English words/phrases → produce a note teaching that expression.
        - Only return success=false when the input is empty, unintelligible, internally contradictory, or impossible to map to any English expression. Short ≠ ambiguous.
        - When you DO return success=false, write error.message in neutral Latin American Spanish (never English, no Iberian forms) and include one concrete suggestion the user can act on (e.g. "El texto está vacío. Escribe una palabra, frase u objetivo comunicativo.").

        Field semantics (shape and enums are enforced by the response schema):
        - expression: a single English target expression — natural, useful, narrow.
        - intended_meaning_es: one natural Spanish meaning (neutral LatAm), not a list.
        - simple_definition_en: short, plain English definition.
        - why_useful: the concrete communicative payoff (no generic "helps you learn English").
        - example_sentence / example_translation: natural example + LatAm Spanish translation; the example MUST support the chosen meaning.
        - usage_pattern: required for note_type = phrase, phrasal_verb, sentence_pattern.
        - cloze_sentence: required for note_type = sentence_pattern; otherwise include only if it adds distinct retrieval value.
        - common_mistake / confusable_with: include only when they add real learner value.
        - source_context: brief paraphrase of the user's situation.
        - cards: 2 to 4 active retrieval cards. Include a recognition card AND a production card whenever each adds retrieval value. Add a cloze card only when it gives distinct value vs. the others.
        - Each card: prompt is specific and unambiguous (never a discussion question); expected_answer is short, objective, ideally one expression; accepted_answers contains only true equivalents, not loose paraphrases; source_field names the note field the card derives from (e.g. expression, example_sentence, cloze_sentence).
        - quality_checks: report each of these codes exactly once with an honest passed/message —
          single_meaning, natural_example, example_supports_meaning, non_ambiguous_answers,
          required_fields_present, clear_card_focus, note_card_alignment.

        Gold example (illustrative — do not copy verbatim; shows the bar for quality):

        For input like user_text = "quiero decir 'no exageres' cuando alguien dramatiza algo", level_band = B1_B2, register = casual, the data payload should look like:

        {
          "note_id": "note_dont_make_a_big_deal",
          "note_type": "phrase",
          "expression": "don't make a big deal out of it",
          "intended_meaning_es": "no exageres / no le des tanta importancia",
          "simple_definition_en": "tell someone to stop treating a small thing as if it were serious or dramatic.",
          "part_of_speech": "chunk",
          "register": "casual",
          "level_band": "B1_B2",
          "domain": "social",
          "why_useful": "lets you defuse drama or push back when someone overreacts to a minor situation — very high frequency in casual conversation.",
          "example_sentence": "It was just a small mistake — don't make a big deal out of it.",
          "example_translation": "Fue solo un error pequeño, no exageres.",
          "usage_pattern": "don't make a big deal out of + [noun / it / what + clause]",
          "common_mistake": "saying 'don't do a big deal' — the verb is 'make', not 'do'.",
          "source_context": "user wants to tell someone to stop dramatizing a small situation",
          "cards": [
            {
              "card_id": "c1",
              "card_type": "recognition",
              "prompt": "What does 'don't make a big deal out of it' mean in casual speech?",
              "expected_answer": "no exageres / no le des tanta importancia",
              "evaluation_mode": "flexible_text",
              "is_active": true,
              "source_field": "expression"
            },
            {
              "card_id": "c2",
              "card_type": "production",
              "prompt": "Casual English phrase to tell someone 'no exageres' (uses the verb 'make' + 'out of it').",
              "expected_answer": "don't make a big deal out of it",
              "evaluation_mode": "exact",
              "is_active": true,
              "source_field": "expression"
            },
            {
              "card_id": "c3",
              "card_type": "cloze",
              "prompt": "It was just a small mistake — don't ___ out of it.",
              "expected_answer": "make a big deal",
              "evaluation_mode": "exact",
              "is_active": true,
              "source_field": "example_sentence"
            }
          ],
          "quality_checks": [
            { "code": "single_meaning", "passed": true, "message": "One meaning, one target expression." },
            { "code": "natural_example", "passed": true, "message": "Reads as real casual speech." },
            { "code": "example_supports_meaning", "passed": true, "message": "The example clearly conveys 'no exageres'." },
            { "code": "non_ambiguous_answers", "passed": true, "message": "Expected answers are short and objective." },
            { "code": "required_fields_present", "passed": true, "message": "All required fields filled for note_type=phrase." },
            { "code": "clear_card_focus", "passed": true, "message": "Each card tests one thing." },
            { "code": "note_card_alignment", "passed": true, "message": "All cards target the same expression and meaning." }
          ]
        }

        If the input is too ambiguous or unusable, return success=false with a short error.message explaining why. Do not include markdown or any text outside the JSON.
        """.trimIndent()
    }

    fun buildExampleRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.generation.GeneratedLearningNote,
    ): String {
        return """
        You are refining one field inside an English learning note for a native Spanish speaker.

        Keep the same learning note and intended meaning.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression.value}"
        - intended_meaning_es: "${note.intendedMeaningEs.value}"
        - simple_definition_en: "${note.simpleDefinitionEn.value}"
        - why_useful: "${note.whyUseful}"
        - current_example_sentence: "${note.exampleSentence}"
        - current_example_translation: "${note.exampleTranslation}"

        Return ONLY valid JSON:
        {
          "success": true,
          "data": {
            "example_sentence": "<new natural example sentence>",
            "example_translation": "<Spanish translation of that sentence>"
          }
        }

        Rules:
        - Keep the same intended meaning and same target expression.
        - The sentence must sound natural.
        - The sentence must support the meaning clearly.
        - Do not return markdown or extra text.
        """.trimIndent()
    }

    fun buildNoteFieldRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.generation.GeneratedLearningNote,
        field: com.emm.domain.generation.RegenerableNoteField,
    ): String {
        val fieldName = when (field) {
            com.emm.domain.generation.RegenerableNoteField.WhyUseful -> "why_useful"
            com.emm.domain.generation.RegenerableNoteField.UsagePattern -> "usage_pattern"
            com.emm.domain.generation.RegenerableNoteField.CommonMistake -> "common_mistake"
        }
        val fieldInstructions = when (field) {
            com.emm.domain.generation.RegenerableNoteField.WhyUseful ->
                "Return a practical reason why this note helps real communication."
            com.emm.domain.generation.RegenerableNoteField.UsagePattern ->
                "Return a concise usage pattern that helps use the expression correctly."
            com.emm.domain.generation.RegenerableNoteField.CommonMistake ->
                "Return one realistic learner mistake to avoid."
        }

        return """
        You are refining exactly one field inside an English learning note for a native Spanish speaker.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression.value}"
        - intended_meaning_es: "${note.intendedMeaningEs.value}"
        - simple_definition_en: "${note.simpleDefinitionEn.value}"
        - example_sentence: "${note.exampleSentence}"
        - usage_pattern: "${note.usagePattern}"
        - common_mistake: "${note.commonMistake}"
        - why_useful: "${note.whyUseful}"

        Field to regenerate:
        - field_name: "$fieldName"

        Return ONLY valid JSON:
        {
          "success": true,
          "data": {
            "$fieldName": "<improved value>"
          }
        }

        Rules:
        - Keep the same target expression and intended meaning.
        - $fieldInstructions
        - Return only the requested field.
        - Do not return markdown or extra text.
        """.trimIndent()
    }

    fun buildClozeRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.generation.GeneratedLearningNote,
    ): String {
        return """
        You are refining one cloze sentence inside an English learning note for a native Spanish speaker.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression.value}"
        - intended_meaning_es: "${note.intendedMeaningEs.value}"
        - simple_definition_en: "${note.simpleDefinitionEn.value}"
        - example_sentence: "${note.exampleSentence}"
        - current_cloze_sentence: "${note.clozeSentence}"

        Return ONLY valid JSON:
        {
          "success": true,
          "data": {
            "cloze_sentence": "<natural cloze sentence with one clear blank for the target expression>"
          }
        }

        Rules:
        - Keep the same target expression and intended meaning.
        - Make the sentence natural and useful.
        - The cloze must test one clear retrieval.
        - Do not return markdown or extra text.
        """.trimIndent()
    }

    fun buildStudyCardRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.generation.GeneratedLearningNote,
        card: com.emm.domain.generation.GeneratedStudyCard,
    ): String {
        val evaluationMode = when (card.evaluationMode) {
            com.emm.domain.generation.EvaluationMode.Exact -> "exact"
            com.emm.domain.generation.EvaluationMode.FlexibleText -> "flexible_text"
            com.emm.domain.generation.EvaluationMode.ManualSelfCheck -> "manual_self_check"
        }
        return """
        You are refining one derived study card inside an English learning note for a native Spanish speaker.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression.value}"
        - intended_meaning_es: "${note.intendedMeaningEs.value}"
        - simple_definition_en: "${note.simpleDefinitionEn.value}"
        - example_sentence: "${note.exampleSentence}"
        - cloze_sentence: "${note.clozeSentence}"

        Card to regenerate:
        - card_id: "${card.cardId}"
        - card_type: "${card.cardType.name.lowercase()}"
        - evaluation_mode: "$evaluationMode"
        - current_prompt: "${card.prompt}"
        - current_expected_answer: "${card.expectedAnswer}"
        - source_field: "${card.sourceField}"

        Return ONLY valid JSON:
        {
          "success": true,
          "data": {
            "card": {
              "card_id": "${card.cardId}",
              "card_type": "${card.cardType.name.lowercase()}",
              "prompt": "<improved retrieval prompt>",
              "expected_answer": "<single expected answer>",
              "evaluation_mode": "$evaluationMode",
              "is_active": true,
              "accepted_answers": ["<optional accepted answer>"],
              "hint": "<optional hint>",
              "explanation": "<optional explanation>",
              "source_field": "${card.sourceField}"
            }
          }
        }

        Rules:
        - Keep the same card type and evaluation mode.
        - Keep the card aligned with the same note meaning.
        - Test one thing only.
        - Do not return markdown or extra text.
        """.trimIndent()
    }
}
