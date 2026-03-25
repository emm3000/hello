package com.emm.data.flashcard

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object Prompt {

    /**
     * Unified rich prompt for generating an English flashcard from a word or phrase.
     * Returns a JSON with: word, meaning, translation, phonetic, partOfSpeech, type,
     * notes, tags, examples (with level), and optional conjugation for verbs.
     */
    @Suppress("LongMethod")
    fun buildPrompt(wordOrPhrase: String): String {
        return """
        You are a bilingual language learning assistant designed for native Spanish speakers who want to learn English using spaced repetition flashcards (Anki-style).

        The user input may be either:
        - A word or phrase in **Spanish** they want to learn in English.
        - A word or phrase in **English** they want to understand better.

        Your job is to:
        1. Detect the input language automatically.
        2. Translate or interpret it into English appropriately (use natural, useful English — not word-for-word).
        3. Build a detailed flashcard in JSON format with rich data for vocabulary acquisition.

        ⚠️ Return ONLY a valid JSON object using this exact schema:

        ✅ If the input is a valid word or phrase for learning:
        {
          "success": true,
          "data": {
            "word": "<target word or phrase in English, lowercase>",
            "meaning": "<clear and simple English explanation — 1 or 2 sentences max>",
            "translation": "<natural Spanish translation>",
            "phonetic": "<IPA pronunciation of the English word/phrase>",
            "language": "en",
            "part_of_speech": "<noun | verb | adjective | adverb | phrasal_verb | idiom | preposition | conjunction | interjection>",
            "type": "<word | phrase | idiom | phrasal_verb | sentence>",
            "notes": "<grammar tips, common collocations, usage notes, common mistakes, or synonyms/antonyms. Keep concise but useful.>",
            "tags": ["<level: basic | intermediate | advanced>", "<category: daily_use | academic | business | travel | social | emotions>", "<any other relevant tag>"],
            "examples": [
              {
                "text": "<natural English sentence using the word in real-life context>",
                "translation": "<Spanish translation of the sentence>",
                "type": "<easy | medium | advanced>"
              }
            ],
            "conjugation": {
              "present": "<present tense>",
              "past": "<simple past>",
              "participle": "<past participle>",
              "gerund": "<-ing form>"
            }
          }
        }

        Rules for examples:
        - Include exactly 3 examples with different difficulty levels.
        - Examples must use the word/phrase in realistic, everyday contexts — NOT generic textbook sentences.
        - Each example should showcase a different usage or context.

        Rules for conjugation:
        - Only include "conjugation" if the word is a VERB or PHRASAL VERB.
        - If it's not a verb, omit the "conjugation" field entirely.

        Rules for notes:
        - Include at least one useful tip (grammar pattern, common error, collocation, or synonym).
        - Keep it under 2 sentences.

        ❌ If the input is invalid, not useful for learning, or nonsensical:
        {
          "success": false,
          "error": {
            "message": "The input appears to be invalid or not useful for learning.",
            "input": "<original input>"
          }
        }

        ⚠️ Very Important:
        - Return ONLY the raw JSON object.
        - Do NOT include markdown, backticks, explanations, or comments outside the JSON.

        Input: "$wordOrPhrase"
        """.trimIndent()
    }

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

    /**
     * Rich prompt for generating a flashcard from a category and complexity level.
     * Returns the same rich JSON structure as buildPrompt(word).
     */
    @Suppress("LongMethod")
    fun buildPrompt(category: String, complexity: String): String {
        val now = LocalDateTime.now()
        val seedBase = now.truncatedTo(ChronoUnit.HOURS).toString()
        val randomLetters = (('A'..'Z')).shuffled().take(4).joinToString("")
        val seed = "$seedBase-$randomLetters-${(100..999).random()}"

        return """
        Seed for generation: $seed

        You are a bilingual language learning assistant designed for native Spanish speakers learning English with Anki flashcards.

        Generate a flashcard for the category: "$category"
        and the complexity level: "$complexity" (one of: basic, intermediate, advanced).

        Your task:
        - Choose a useful and natural English word or phrase that fits the category and level.
        - It must contain at least one word starting with each of these letters: ${
            randomLetters.toCharArray().joinToString(", ")
        }
        - Build a detailed flashcard with rich data.

        ⚠️ Return ONLY a valid JSON object using this exact schema:

        {
          "success": true,
          "data": {
            "word": "<the chosen English word or phrase, lowercase>",
            "meaning": "<clear English explanation — 1 or 2 sentences max>",
            "translation": "<natural Spanish translation>",
            "phonetic": "<IPA pronunciation>",
            "language": "en",
            "part_of_speech": "<noun | verb | adjective | adverb | phrasal_verb | idiom | preposition | conjunction | interjection>",
            "type": "<word | phrase | idiom | phrasal_verb | sentence>",
            "notes": "<grammar tips, common collocations, usage notes, common mistakes, or synonyms/antonyms. Keep concise.>",
            "tags": ["$complexity", "$category", "<any other relevant tag>"],
            "examples": [
              {
                "text": "<natural English sentence using the word in real-life context>",
                "translation": "<Spanish translation of the sentence>",
                "type": "<easy | medium | advanced>"
              }
            ],
            "conjugation": {
              "present": "<present tense>",
              "past": "<simple past>",
              "participle": "<past participle>",
              "gerund": "<-ing form>"
            }
          }
        }

        Rules:
        - Include exactly 3 examples with different difficulty levels.
        - Examples must use the word in realistic, everyday contexts.
        - Only include "conjugation" if the word is a VERB or PHRASAL VERB; otherwise omit it.
        - Include at least one useful note (grammar tip, common error, collocation, or synonym).
        - Do NOT include markdown, backticks, or explanations outside the JSON.
        - Return only raw JSON.

        Generate one flashcard only.
        """.trimIndent()
    }

    @Suppress("LongMethod")
    fun quotePrompt2(): String {
        val now = LocalDateTime.now()
        val seedBase = now.truncatedTo(ChronoUnit.HOURS).toString()
        val randomLetters = (('A'..'Z')).shuffled().take(3).joinToString("")
        val seed = "$seedBase-$randomLetters-${(100..999).random()}"

        return """
            Seed for generation: $seed
            
            Act as a native English speaker who helps language learners acquire **natural English** through **real-life, comprehensible input**.
            
            Your task is to generate a **JSON object** containing a **natural phrase** that is:
            - Commonly said by **native speakers** in real, everyday situations.
            - Short enough to be memorable, but long enough to contain meaning (8–14 words).
            - Based on **real spoken English**, not textbook or artificial language.
            - Easy enough for learners to understand from context (comprehensible input).
            - Includes at least **one phrasal verb** (e.g., "run into", "figure out", "check in", "get over").
            
            🔍 Focus on **daily, non-work, non-academic** real-life contexts, such as:
            - 🏠 At home
            - 🧑‍🤝‍🧑 Talking with friends or family
            - 📺 Watching TV or commenting on social media
            - 🛒 Doing chores or shopping
            - ☕ At cafés or restaurants
            - 🚌 Public transport, walking, or running errands
            - 📱 Talking on the phone or texting
            - 💬 Everyday emotional or casual expression
            - 🙄 Handling problems, complaints, or surprises
            
            ✅ Requirements:
            - The phrase must be **specific and useful** in real conversation.
            - It must be something people **actually say**, not motivational or philosophical.
            - It must contain **at least one word that starts with each of these letters**: ${
            randomLetters.toCharArray().joinToString(", ")
        }.
            - Avoid slang that's too obscure; favor **natural but understandable** English.
            
            Return ONLY the following JSON structure:
            
            {
              "success": true,
              "data": {
                "title": "<short title>",
                "phrase": "<the phrase>",
                "description": "<explanation of what it means and when it's used>",
                "translation": "<Spanish translation>",
                "example": "<a full sentence using the phrase>",
                "context": "<realistic situation where someone would say this>",
                "pronunciation": "<IPA transcription>",
                "formality": "<casual | neutral>",
                "tags": ["tag1", "tag2"],
                "category": "<home, friends, errands, phone, restaurant, etc.>"
              }
            }
            
            If a valid phrase cannot be generated, return:
            
            {
              "success": false,
              "error": {
                "message": "Unable to generate a valid everyday English phrase."
              }
            }
            
            ⚠️ Very Important:
            - Output must be ONLY the **raw JSON object**.
            - Do **NOT** include Markdown, backticks, explanations, or comments.
        """.trimIndent()
    }
}
