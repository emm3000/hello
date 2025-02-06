package com.emm.data.scrap

import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity
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
    private val wordDao: WordDao,
) : WordContentRepository {

    override suspend fun createThenSave(word: Word) {
        val wordContent: WordContentHolder = oxfordScrapper.scrap(word)
        save(wordContent, word)
        updateIfWordHasContent(word)
    }

    private suspend fun updateIfWordHasContent(word: Word) {
        val selectBy: WordEntity = wordDao.selectBy(word.id) ?: return
        wordDao.update(selectBy.copy(hasContent = true))
    }

    override suspend fun fetchContent(word: String): WordContent? = withContext(Dispatchers.IO) {
        val wordContentWithExamples: WordContentWithExamples = wordContentDao.fetchContentWord(word)
            ?: return@withContext null
        return@withContext mapToWordContent(wordContentWithExamples)
    }

    private fun mapToWordContent(wordContentWithExamples: WordContentWithExamples) = WordContent(
        wordId = wordContentWithExamples.wordContentEntity.id,
        word = wordContentWithExamples.wordContentEntity.wordFromScrap,
        pos = wordContentWithExamples.wordContentEntity.pos,
        examples = wordContentWithExamples.exampleEntities.map(::mapToExample)
    )

    private fun mapToExample(exampleEntity: ExampleEntity) = Example(
        number = exampleEntity.number,
        title = exampleEntity.title,
        sentences = exampleEntity.sentences.split("|").filter(String::isNotBlank)
    )

    private suspend fun save(content: WordContentHolder, word: Word) {
        val g = UUID.randomUUID().toString()
        val wordContentEntity = WordContentEntity(
            id = g,
            wordId = word.id,
            wordFromScrap = content.wordFromScrap,
            pos = content.pos,
        )
        wordContentDao.insert(wordContentEntity)

        val exampleEntities: List<ExampleEntity> = content.examples.map { example ->
            mapToEntity(example, g)
        }
        exampleDao.upsert(exampleEntities)
    }

    private fun mapToEntity(example: ExampleHolder, contentEntityId: String): ExampleEntity {
        val sentences: String = example.sentences.joinToString(separator = "|")
        return ExampleEntity(
            id = UUID.randomUUID().toString(),
            number = example.number,
            title = example.title,
            sentences = sentences,
            contentId = contentEntityId,
        )
    }
}