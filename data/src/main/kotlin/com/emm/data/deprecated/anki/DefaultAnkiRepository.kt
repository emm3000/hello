package com.emm.data.deprecated.anki

import com.emm.data.deprecated.wordcontent.PromptFactory
import com.emm.data.flashcard.GeminiService
import com.emm.domain.deprecated.anki.AnkiRepository
import com.emm.domain.deprecated.word.SourceType
import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordContent
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