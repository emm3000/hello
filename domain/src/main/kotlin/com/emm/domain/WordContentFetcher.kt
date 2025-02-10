package com.emm.domain

class WordContentFetcher(private val repository: WordContentRepository) {

    suspend fun fetch(wordId: String): HolderOfWordContent {
        val wordContents = repository.fetchContentBy(wordId)
        val (ia, scrap) = wordContents.partition(::isIaContent)
        return HolderOfWordContent(
            iaContent = ia.getOrNull(0),
            scrapContent = scrap.getOrNull(0),
        )
    }

    private fun isIaContent(wordContent: WordContent): Boolean = wordContent.sourceType == SourceType.IA

    data class HolderOfWordContent(
        val iaContent: WordContent?,
        val scrapContent: WordContent?,
    )
}