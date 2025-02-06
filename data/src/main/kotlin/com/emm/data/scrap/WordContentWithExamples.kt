package com.emm.data.scrap

import androidx.room.Embedded
import androidx.room.Relation

data class WordContentWithExamples(
    @Embedded val wordContent: WordContentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contentId",
    )
    val examples: List<ExampleEntity>
)
