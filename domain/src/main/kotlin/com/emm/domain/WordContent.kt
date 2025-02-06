package com.emm.domain

data class WordContent(
    val wordId: String,
    val word: String,
    val pos: String,
    val examples: List<Example>,
)