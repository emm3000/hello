package com.emm.data.deprecated.wordcontent

import com.emm.domain.deprecated.word.SourceType
import com.emm.domain.deprecated.word.Word
import com.emm.domain.deprecated.word.WordContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OxfordScrapper {

    suspend fun scrap(word: Word): WordContent = withContext(Dispatchers.IO) {
        return@withContext WordContent(
            wordContentId = "mnesarchum",
            word = "facilisis",
            pos = "conceptam",
            sourceType = SourceType.SCRAPPING,
            examples = listOf()
        )
    }
}