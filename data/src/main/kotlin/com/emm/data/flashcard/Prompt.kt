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
