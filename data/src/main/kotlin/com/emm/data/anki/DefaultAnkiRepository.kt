package com.emm.data.anki

import com.emm.data.wordcontent.GeminiService
import com.emm.data.wordcontent.PromptFactory
import com.emm.domain.anki.Anki
import com.emm.domain.anki.AnkiRepository
import java.util.UUID

class DefaultAnkiRepository(private val geminiService: GeminiService) : AnkiRepository {

    override suspend fun createCard(input: String): Anki {
        val ankiPrompt: String = PromptFactory.ankiPrompt(input)
        val result: String = geminiService.process(ankiPrompt)
        return Anki(UUID.randomUUID().toString(), result)
    }
}