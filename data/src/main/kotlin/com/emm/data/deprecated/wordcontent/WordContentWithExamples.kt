package com.emm.data.deprecated.wordcontent

import androidx.room.Embedded
import androidx.room.Relation

data class WordContentWithExamples(
    @Embedded val wordContentEntity: WordContentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contentId",
    )
    val exampleEntities: List<ExampleEntity>
)
