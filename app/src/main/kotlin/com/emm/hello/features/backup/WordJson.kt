package com.emm.hello.features.backup

import com.emm.data.deprecated.word.WordEntity
import com.emm.data.deprecated.wordcontent.ExampleEntity
import com.emm.data.deprecated.wordcontent.WordContentEntity
import kotlinx.serialization.Serializable

@Serializable
data class WordJson(
    val wordEntities: List<WordEntity>,
    val wordContentEntities: List<WordContentEntity>,
    val exampleEntities: List<ExampleEntity>,
)