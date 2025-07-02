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
                  // Include at least 10 examples with different types
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
            - Do NOT include any markdown formatting (no backticks).
            - Do NOT include any explanation or extra text before or after the JSON.
            
            User input: "$wordOrPhrase"
            
            Respond with the appropriate JSON only.
        """.trimIndent()
        return fullPrompt
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
        - The phrase must contain at least **one word that starts with each of these letters**: ${randomLetters.toCharArray().joinToString(", ")}.

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
}