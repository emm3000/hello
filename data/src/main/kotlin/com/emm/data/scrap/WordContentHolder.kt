package com.emm.data.scrap

data class WordContentHolder(
    val wordFromScrap: String,
    val pos: String,
    val examples: List<ExampleHolder>,
)