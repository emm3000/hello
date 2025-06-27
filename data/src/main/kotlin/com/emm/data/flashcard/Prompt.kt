package com.emm.data.flashcard

object Prompt {

    fun buildPrompt(wordOrPhrase: String): String {
        val fullPrompt = """
            Act as a smart English language assistant that helps users learn vocabulary and phrases.

            You will receive an input word or phrase in English.

            Your task is to return a well-formatted JSON with the following rules:
            ✅ If the input is a valid English word or phrase, respond with:

            {
              "success": true,
              "data": {
                "word": "<original input in lowercase>",
                "meaning": "<clear English explanation of the word/phrase>",
                "translation": "<translation to Spanish>",
                "example": "<a short, natural sentence using the word or phrase in context>",
                "phonetic": "<IPA pronunciation of the word or phrase>",
                "language": "en",
                "audio_url": null,
                "image_prompt": "<a short English description of what image could represent this word or phrase>"
              }
            }

            ❌ If the input is invalid, not in English, or nonsensical (e.g. gibberish), respond with:
            
            {
              "success": false,
              "error": {
                "message": "The input appears to be invalid or nonsensical.",
                "input": "<original input>"
              }
            }

            User input: "$wordOrPhrase"

            Respond with the appropriate JSON only.
        """.trimIndent()
        return fullPrompt
    }
}