package com.emm.data.flashcard

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
}