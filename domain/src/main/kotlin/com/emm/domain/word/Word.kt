package com.emm.domain.word

data class Word(
    val id: String,
    val word: String,
    val hasContent: Boolean,
    val createdAt: Long,
)