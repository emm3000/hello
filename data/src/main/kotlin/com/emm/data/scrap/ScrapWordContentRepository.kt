package com.emm.data.scrap

import com.emm.domain.Example
import com.emm.domain.Word
import com.emm.domain.WordContent
import com.emm.domain.WordContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ScrapWordContentRepository(
    private val oxfordScrapper: OxfordScrapper,
    private val wordContentDao: WordContentDao,
    private val exampleDao: ExampleDao,
) : WordContentRepository {

    override suspend fun createThenSave(word: Word) {
        val wordContent: WordContent = oxfordScrapper.scrap(word)
        save(wordContent)
    }

    override suspend fun fetchContent(word: String): WordContent? = withContext(Dispatchers.IO) {
        val wordContentWithExamples: WordContentWithExamples = wordContentDao.fetchContentWord(word)
            ?: return@withContext null
        return@withContext mapToWordContent(wordContentWithExamples)
    }

    private fun mapToWordContent(wordContentWithExamples: WordContentWithExamples) = WordContent(
        wordId = wordContentWithExamples.wordContent.wordId,
        word = wordContentWithExamples.wordContent.word,
        pos = wordContentWithExamples.wordContent.pos,
        examples = wordContentWithExamples.examples.map(::mapToExample)
    )

    private fun mapToExample(exampleEntity: ExampleEntity) = Example(
        number = exampleEntity.number,
        title = exampleEntity.title,
        sentences = exampleEntity.sentences.split("|")
    )

    private suspend fun save(content: WordContent) {
        val contentEntityId: String = UUID.randomUUID().toString()
        val wordContentEntity = WordContentEntity(
            id = contentEntityId,
            word = content.word,
            pos = content.pos,
            wordId = content.wordId
        )
        wordContentDao.insert(wordContentEntity)

        val exampleEntities: List<ExampleEntity> = content.examples.map { example ->
            mapToEntity(example, contentEntityId)
        }
        exampleDao.upsert(exampleEntities)
    }

    private fun mapToEntity(example: Example, contentEntityId: String) = ExampleEntity(
        id = UUID.randomUUID().toString(),
        number = example.number,
        title = example.title,
        sentences = example.sentences.joinToString { "|" },
        contentId = contentEntityId,
    )
}