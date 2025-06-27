package com.emm.domain.deprecated.word

class WordContentFetcher(private val repository: WordContentRepository) {

    suspend fun fetch(wordId: String): HolderOfWordContent {
        val wordContents = repository.fetchContentBy(wordId)
        val sourceTypeListMap: Map<SourceType, List<WordContent>> = wordContents.groupBy(WordContent::sourceType)
        return HolderOfWordContent(
            iaContent = sourceTypeListMap[SourceType.IA]?.firstOrNull(),
            scrapContent = sourceTypeListMap[SourceType.SCRAPPING]?.firstOrNull(),
            iaAnkiContent = sourceTypeListMap[SourceType.IA_ANKI]?.firstOrNull(),
        )
    }

    data class HolderOfWordContent(
        val iaContent: WordContent?,
        val scrapContent: WordContent?,
        val iaAnkiContent: WordContent?,
    )
}