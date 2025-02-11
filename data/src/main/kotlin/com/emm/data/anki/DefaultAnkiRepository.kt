package com.emm.data.anki

import com.emm.data.wordcontent.GeminiService
import com.emm.data.wordcontent.PromptFactory
import com.emm.domain.anki.AnkiRepository
import com.emm.domain.word.SourceType
import com.emm.domain.word.Word
import com.emm.domain.word.WordContent
import java.util.UUID

class DefaultAnkiRepository(private val geminiService: GeminiService) : AnkiRepository {

    override suspend fun createCard(word: Word): WordContent {
        val ankiPrompt: String = PromptFactory.ankiPrompt(word.word)
        val plainText: String = geminiService.process(ankiPrompt)
        return WordContent(
            wordContentId = UUID.randomUUID().toString(),
            word = word.word,
            pos = plainText,
            sourceType = SourceType.IA_ANKI,
            examples = emptyList(),
        )
    }
}