package com.emm.data.flashcard

object Prompt {

    @Suppress("LongMethod")
    fun buildLearningNotePrompt(input: com.emm.domain.flashcard.FlashcardGenerationInput): String {
        val communicativeIntent = com.emm.domain.flashcard.communicativeIntents
            .firstOrNull { it.id == input.communicativeIntentId }

        return """
        You are a bilingual English-learning assistant for native Spanish speakers.

        Your task is to generate exactly one structured learning note for spaced repetition.

        The output must represent:
        - one intended meaning only
        - one target English expression only
        - multiple derived study cards for retrieval practice

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

        Return ONLY a valid JSON object using this schema:

        {
          "success": true,
          "data": {
            "note_id": "<stable note id placeholder or generated id>",
            "note_type": "<word | phrase | phrasal_verb | idiom | sentence_pattern>",
            "expression": "<single English target expression>",
            "intended_meaning_es": "<one natural Spanish meaning only>",
            "simple_definition_en": "<short simple English definition>",
            "part_of_speech": "<noun | verb | adjective | adverb | preposition | conjunction | interjection | phrasal_verb | idiom | chunk | other>",
            "register": "<casual | neutral | formal>",
            "level_band": "<A1_A2 | B1_B2 | C1_PLUS>",
            "domain": "<daily_life | travel | social | work | study | media | mixed>",
            "why_useful": "<practical reason why this helps real communication>",
            "example_sentence": "<natural example sentence>",
            "example_translation": "<Spanish translation of the example sentence>",
            "lemma": "<optional base form>",
            "ipa": "<optional IPA>",
            "usage_pattern": "<optional usage pattern if relevant>",
            "irregular_forms": ["<optional form>"],
            "collocations": ["<optional collocation>"],
            "common_mistake": "<optional learner warning>",
            "confusable_with": ["<optional confusable item>"],
            "cloze_sentence": "<optional cloze sentence>",
            "source_context": "<optional context derived from input>",
            "warnings": ["<optional warning>"],
            "cards": [
              {
                "card_id": "<id>",
                "card_type": "<recognition | production | cloze | form>",
                "prompt": "<single retrieval prompt>",
                "expected_answer": "<single expected answer>",
                "evaluation_mode": "<exact | flexible_text | manual_self_check>",
                "is_active": true,
                "accepted_answers": ["<optional accepted answer>"],
                "hint": "<optional hint>",
                "explanation": "<optional explanation>",
                "source_field": "<field name>"
              }
            ],
            "quality_checks": [
              {
                "code": "<single_meaning | natural_example | example_supports_meaning | non_ambiguous_answers | required_fields_present | clear_card_focus | note_card_alignment>",
                "passed": true,
                "message": "<short explanation>"
              }
            ]
          }
        }

        Rules:
        - Return one note only.
        - The note must target one intended meaning only.
        - The English expression must be natural and useful.
        - The example sentence must sound natural, not textbook-like.
        - The example must support the chosen intended meaning.
        - Every card must test one clear thing only.
        - If the note_type is phrase or phrasal_verb, include usage_pattern.
        - If the note_type is sentence_pattern, include usage_pattern and cloze_sentence.
        - Include at least the required cards for the note type.
        - Include a quality check for single_meaning.
        - Do not include markdown, explanations, or text outside the JSON.

        If the input is too ambiguous or unusable, return:

        {
          "success": false,
          "error": {
            "message": "The input is too ambiguous or insufficient to generate a high-value learning note."
          }
        }
        """.trimIndent()
    }

    fun buildExampleRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.flashcard.GeneratedLearningNote,
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
        - expression: "${note.expression}"
        - intended_meaning_es: "${note.intendedMeaningEs}"
        - simple_definition_en: "${note.simpleDefinitionEn}"
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

    fun buildClozeRegenerationPrompt(
        input: com.emm.domain.flashcard.FlashcardGenerationInput,
        note: com.emm.domain.flashcard.GeneratedLearningNote,
    ): String {
        return """
        You are refining one cloze sentence inside an English learning note for a native Spanish speaker.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression}"
        - intended_meaning_es: "${note.intendedMeaningEs}"
        - simple_definition_en: "${note.simpleDefinitionEn}"
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
        note: com.emm.domain.flashcard.GeneratedLearningNote,
        card: com.emm.domain.flashcard.GeneratedStudyCard,
    ): String {
        val evaluationMode = when (card.evaluationMode) {
            com.emm.domain.flashcard.EvaluationMode.Exact -> "exact"
            com.emm.domain.flashcard.EvaluationMode.FlexibleText -> "flexible_text"
            com.emm.domain.flashcard.EvaluationMode.ManualSelfCheck -> "manual_self_check"
        }
        return """
        You are refining one derived study card inside an English learning note for a native Spanish speaker.

        Input data:
        - input_type: "${input.inputType.name}"
        - user_text: "${input.userText}"
        - intended_meaning_es: "${input.intendedMeaningEs}"
        - context_sentence: "${input.contextSentence}"

        Current note:
        - expression: "${note.expression}"
        - intended_meaning_es: "${note.intendedMeaningEs}"
        - simple_definition_en: "${note.simpleDefinitionEn}"
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
