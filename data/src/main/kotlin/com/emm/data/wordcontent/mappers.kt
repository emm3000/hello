package com.emm.data.wordcontent

import com.emm.domain.Example
import com.emm.domain.SourceType
import com.emm.domain.WordContent
import java.util.UUID

fun mapToExampleEntities(example: Example, contentEntityId: String): ExampleEntity {
    val sentences: String = example.sentences.joinToString(separator = "|")
    return ExampleEntity(
        id = UUID.randomUUID().toString(),
        number = example.number,
        title = example.title,
        sentences = sentences,
        contentId = contentEntityId,
    )
}

fun mapToWordContents(wordContentWithExamples: List<WordContentWithExamples>): List<WordContent> = wordContentWithExamples.map(::mapToWordContent)

fun mapToWordContent(wordContentWithExamples: WordContentWithExamples) = WordContent(
    wordContentId = wordContentWithExamples.wordContentEntity.id,
    word = wordContentWithExamples.wordContentEntity.wordFromScrap,
    pos = wordContentWithExamples.wordContentEntity.pos,
    sourceType = SourceType.valueOf(wordContentWithExamples.wordContentEntity.sourceType),
    examples = wordContentWithExamples.exampleEntities.map(::convertEntityToDomain)
)

fun convertEntityToDomain(exampleEntity: ExampleEntity) = Example(
    number = exampleEntity.number,
    title = exampleEntity.title,
    sentences = exampleEntity.sentences.split("|").filter(String::isNotBlank)
)