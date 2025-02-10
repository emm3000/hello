package com.emm.domain

class WordContentCreator(
    private val wordContentRepository: WordContentRepository,
    private val wordRepository: WordRepository,
) {

    suspend fun create(word: Word, sourceType: SourceType) {
        val wordContent: WordContent = when (sourceType) {
            SourceType.SCRAPPING -> wordContentRepository.createScrappingContent(word)
            SourceType.IA -> wordContentRepository.createIAContent(word)
        }
        wordContentRepository.saveContent(wordContent, word.id)
        wordRepository.updateHasContent(word, true)
    }
}