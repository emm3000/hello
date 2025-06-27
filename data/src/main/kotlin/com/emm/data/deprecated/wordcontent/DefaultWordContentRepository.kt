package com.emm.data.deprecated.wordcontent

import com.emm.domain.word.SourceType
import com.emm.domain.word.Word
import com.emm.domain.word.WordContent
import com.emm.domain.word.WordContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class DefaultWordContentRepository(
    private val oxfordScrapper: OxfordScrapper,
    private val geminiService: GeminiService,
    private val wordContentDao: WordContentDao,
    private val exampleDao: ExampleDao,
) : WordContentRepository {

    override suspend fun createScrappingContent(word: Word): WordContent = oxfordScrapper.scrap(word)

    override suspend fun createIAContent(word: Word): WordContent {
        val dictionaryPrompt = PromptFactory.dictionaryPrompt(word.word)
        val process = geminiService.process(dictionaryPrompt)
        return WordContent(
            wordContentId = UUID.randomUUID().toString(),
            word = word.word,
            pos = process,
            sourceType = SourceType.IA,
            examples = listOf()
        )
    }

    override suspend fun saveContent(wordContent: WordContent, wordId: String) = withContext(Dispatchers.IO) {
        val wordContentEntity = WordContentEntity(
            id = wordContent.wordContentId,
            wordFromScrap = wordContent.word,
            pos = wordContent.pos,
            sourceType = wordContent.sourceType.name,
            wordId = wordId,
        )
        wordContentDao.insert(wordContentEntity)

        val exampleEntities: List<ExampleEntity> = wordContent.examples.map { example ->
            mapToExampleEntities(example, wordContent.wordContentId)
        }
        exampleDao.upsert(exampleEntities)
    }

    override suspend fun fetchContentBy(wordId: String): List<WordContent> = withContext(Dispatchers.IO) {
        val wordContentWithExamples: List<WordContentWithExamples> = wordContentDao.fetchContentWord(wordId)
        mapToWordContents(wordContentWithExamples)
    }
}