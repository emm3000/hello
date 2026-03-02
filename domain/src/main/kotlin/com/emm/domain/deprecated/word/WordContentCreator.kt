package com.emm.domain.deprecated.word

import com.emm.domain.deprecated.anki.AnkiRepository

class WordContentCreator(
    private val wordContentRepository: WordContentRepository,
    private val wordRepository: WordRepository,
    private val ankiRepository: AnkiRepository,
) {

    suspend fun create(word: Word, sourceType: SourceType) {
        val wordContent: WordContent = when (sourceType) {
            SourceType.SCRAPPING -> wordContentRepository.createScrappingContent(word)
            SourceType.IA -> wordContentRepository.createIAContent(word)
            SourceType.IA_ANKI -> ankiRepository.createCard(word)
        }
        wordContentRepository.saveContent(wordContent, word.id)
        wordRepository.updateHasContent(word, true)
    }
}
