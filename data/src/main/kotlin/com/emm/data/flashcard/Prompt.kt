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
        val seed: String = now.truncatedTo(ChronoUnit.HOURS).toString() + "_${(0..1000).random()}"

        val fullPrompt = """
            Seed for generation: $seed
            
            Act as an English language assistant specialized in real-world software development communication.
            
            Your task is to generate a JSON object that contains a phrase commonly used by developers in real-world work settings (e.g., team meetings, code reviews, version control discussions, debugging sessions, or task coordination).

            The phrase should follow these quality guidelines:
            
            - ✅ It must be **naturally spoken or written** by software engineers during real interactions.
            - ✅ It must be **long enough** to sound realistic (at least 7–10 words), not too short or generic.
            - ✅ It should reflect a **specific and meaningful situation**, such as:
              - Asking for help or clarification
              - Explaining a technical decision
              - Giving or receiving feedback
              - Describing a bug, a merge conflict, or a code smell
              - Planning or estimating tasks
            - ✅ It should vary in structure and vocabulary from previously common phrases.
            - ⚠️ Avoid repeating simple or generic phrases like "Can you review this?" or "Let's fix this bug."
            
            Return the result in this JSON format:
            
            {
              "success": true,
              "data": {
                "title": "<short title>",
                "phrase": "<the phrase>",
                "description": "<explanation of what it means and when it's used>",
                "translation": "<Spanish translation>",
                "example": "<a full sentence using the phrase>",
                "context": "<describe a realistic situation where this is said>",
                "pronunciation": "<IPA transcription>",
                "formality": "<casual | formal | neutral>",
                "tags": ["tag1", "tag2"],
                "category": "<general topic like 'code review', 'communication', etc.>"
              }
            }
            
            If the phrase cannot be generated or validated, respond with:
            
            {
              "success": false,
              "error": {
                "message": "Unable to generate a valid software development phrase."
              }
            }
            
            ⚠️ Very Important:
            - Only return a valid JSON object.
            - Do NOT include any markdown formatting (no backticks).
            - Do NOT include any explanation or extra text before or after the JSON.
            
            Respond with the appropriate JSON only.
        """.trimIndent()
        return fullPrompt
    }
}