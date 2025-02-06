package com.emm.data.scrap

import com.emm.data.word.WordDao
import com.emm.domain.Word
import com.emm.domain.WordContent
import com.emm.domain.WordContentRepository

class ScrapWordContentRepository(
    private val oxfordScrapper: OxfordScrapper,
    private val wordDao: WordDao,
): WordContentRepository {

    override suspend fun create(word: Word) {
        oxfordScrapper.scrap(word)
    }

    override suspend fun save(content: WordContent) {
        TODO("Not yet implemented")
    }
}