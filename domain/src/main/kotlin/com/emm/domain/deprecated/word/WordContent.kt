package com.emm.domain.deprecated.word

data class WordContent(
    val wordContentId: String,
    val word: String,
    val pos: String,
    val sourceType: SourceType,
    val examples: List<Example>,
)
