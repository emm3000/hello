package com.emm.data.flashcard

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object Prompt {

    fun buildPrompt(wordOrPhrase: String): String {
        val fullPrompt = """
            Act as a smart English language assistant that helps users learn vocabulary and phrases.

            You will receive an input word or phrase in English from the user.
            
            Your task is to return a well-formatted JSON with the following structure:
            
            ✅ If the input is a valid English word or phrase, respond with:
            {
              "success": true,
              "data": {
                "word": "<original input in lowercase>",
                "meaning": "<a clear and simple English explanation of the word or phrase>",
                "translation": "<the translation to Spanish>",
                "phonetic": "<IPA pronunciation of the word or phrase>",
                "language": "en",
                "audio_url": null,
                "image_prompt": "<a short English description of an image that could represent the word or phrase>",
                "examples": [
                  {
                    "text": "<short natural sentence using the word or phrase in context>",
                    "translation": "<Spanish translation of the sentence>",
                    "type": "<easy | medium | daily_use | advanced>"
                  },
                  ...
                  // Include at least 3 examples with different types
                ]
              }
            }
            
            ❌ If the input is invalid, not in English, or nonsensical (e.g., random characters or gibberish), respond with:
            {
              "success": false,
              "error": {
                "message": "The input appears to be invalid or nonsensical.",
                "input": "<original input>"
              }
            }
            
            ⚠️ Very Important:
            - Only return a valid JSON object.
            
            User input: "$wordOrPhrase"
            
            Respond with the appropriate JSON only.
        """.trimIndent()
        return fullPrompt
    }

    fun buildPrompt2(wordOrPhrase: String): String {
        return """
        You are a bilingual language learning assistant designed for native Spanish speakers who want to learn English using spaced repetition flashcards.

        The user input may be either:
        - A word or phrase in **Spanish** they want to learn in English.
        - A word or phrase in **English** they want to understand better.

        Your job is to:
        1. Detect the input language automatically.
        2. Translate or interpret it into English appropriately (not word-for-word—use natural, useful English).
        3. Build a detailed Anki flashcard in JSON format with rich data for vocabulary acquisition.

        ⚠️ Ensure the output uses the following schema:

        {
          "success": true,
          "detected_language": "<es | en>",
          "type": "<word | phrase | idiom | sentence | phrasal_verb>",
          "data": {
            "word": "<target word or phrase in English>",
            "meaning": "<clear English explanation of the word or phrase>",
            "translation": "<Spanish translation>",
            "phonetic": "<IPA pronunciation of the English term>",
            "language": "en",
            "audio_url": null,
            "image_prompt": "<a prompt to generate an image that represents the meaning>",
            "tags": ["<basic>", "<daily_use>", "<verb>", "<intermediate>", "<idiom>", ...],
            "examples": [
              {
                "text": "<English sentence using the word/phrase>",
                "translation": "<Spanish translation>",
                "level": "<easy | medium | advanced>"
              }
            ],
            "notes": "<grammar tips, collocations, common errors, or synonyms>",
            "conjugation": {
              "present": "<present>",
              "past": "<past>",
              "participle": "<past participle>",
              "gerund": "<-ing>"
            } // Only include if it's a verb
          }
        }

        If the input is invalid, nonsensical, or not useful for learning English, return:
        {
          "success": false,
          "error": {
            "message": "Invalid or unclear input for language learning.",
            "input": "<original input>"
          }
        }

        Return only a valid JSON object. No markdown, no extra text.

        Input: "$wordOrPhrase"
        """.trimIndent()
    }

    fun quotePrompt(): String {
        val now = LocalDateTime.now()
        val seedBase = now.truncatedTo(ChronoUnit.HOURS).toString()
        val randomLetters = (('A'..'Z')).shuffled().take(3).joinToString("")
        val seed = "$seedBase-$randomLetters-${(100..999).random()}"

        return """
        Seed for generation: $seed

        Act as an English learning assistant specialized in **everyday real-world conversation** across multiple life contexts.

        Your task is to generate a JSON object containing a **natural phrase commonly used in real daily life**, which could be used in any of the following contexts:

        - 🏠 At home  
        - 🧑‍🤝‍🧑 With friends or social gatherings  
        - 🏢 In the office or remote work  
        - 🛒 Shopping in stores or supermarkets  
        - ✈️ Traveling (airports, hotels, check-ins)  
        - 🍽️ At restaurants or cafés  
        - 📞 On the phone or customer service  
        - 🎓 School, university, or study-related  
        - 💬 Any real-world social or practical scenario

        ✅ Quality constraints:

        - The phrase must sound **natural, specific, and useful**.
        - It must be something people **actually say** in casual or professional conversation.
        - It must contain at least **one phrasal verb** (e.g., "figure out", "check in", "run into", "look forward to").
        - The phrase should have at least **8–12 words**, not too short.
        - ⚠️ Avoid generic textbook phrases or motivational quotes.

        🔠 To ensure variety and prevent repetition:
        - The phrase must contain at least **one word that starts with each of these letters**: ${
            randomLetters.toCharArray().joinToString(", ")
        }.

        Return only the following valid JSON format:

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
            "formality": "<casual | formal | neutral>",
            "tags": ["tag1", "tag2"],
            "category": "<context category like 'restaurant', 'airport', 'home', 'friends', etc.>"
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
        - Only return the **JSON object**.
        - Do **NOT include** markdown, backticks, explanations, or comments outside the JSON.
        """.trimIndent()
    }

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

    fun buildPrompt(category: String, complexity: String): String {
        val now = LocalDateTime.now()
        val seedBase = now.truncatedTo(ChronoUnit.HOURS).toString()
        val randomLetters = (('A'..'Z')).shuffled().take(4).joinToString("")
        val seed = "$seedBase-$randomLetters-${(100..999).random()}"
        return """
            Seed for generation: $seed
            Act as an English teacher that creates simple and effective flashcards for Spanish-speaking learners using the Anki format.
    
            The flashcard should be based on the category: "$category"
            and the complexity level: "$complexity" (one of: basic, intermediate, advanced).
    
            Your task:
            - Choose a useful and natural English sentence or phrase that fits the category and level.
            - Translate it to Spanish clearly.
            - Return a minimal JSON object with ONLY the essential info for an Anki card.
            - It must contain **at least one word that starts with each of these letters**: ${
            randomLetters.toCharArray().joinToString(", ")
        }
    
            Format:
            {
              "success": true,
              "category": "<category>",
              "complexity": "<complexity>",
              "front": "<english phrase or sentence>",
              "back": "<spanish translation>"
            }
    
            ❌ Do NOT include examples, IPA, audio, tags, explanations or any extra fields.
            ❌ Do NOT include markdown, prefaces, or notes.
            ✅ Return only raw JSON in the format above.
    
            Generate one flashcard only.
        """.trimIndent()
    }
}
