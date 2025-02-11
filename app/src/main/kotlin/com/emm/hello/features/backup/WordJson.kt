package com.emm.hello.features.backup

import com.emm.data.word.WordEntity
import com.emm.data.wordcontent.ExampleEntity
import com.emm.data.wordcontent.WordContentEntity
import kotlinx.serialization.Serializable

@Serializable
data class WordJson(
    val wordEntities: List<WordEntity>,
    val wordContentEntities: List<WordContentEntity>,
    val exampleEntities: List<ExampleEntity>,
)