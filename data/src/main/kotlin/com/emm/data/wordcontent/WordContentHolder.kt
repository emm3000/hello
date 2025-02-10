package com.emm.data.wordcontent

data class WordContentHolder(
    val wordFromScrap: String,
    val pos: String,
    val examples: List<ExampleHolder>,
)