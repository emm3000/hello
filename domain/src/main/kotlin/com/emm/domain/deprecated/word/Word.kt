package com.emm.domain.deprecated.word

import java.time.Instant
import java.util.UUID

data class Word(
    val id: String,
    val word: String,
    val hasContent: Boolean,
    val createdAt: Long,
) {

    companion object {

        fun create(word: String, hasContent: Boolean): Word = Word(
            id = UUID.randomUUID().toString(),
            word = word,
            hasContent = hasContent,
            createdAt = Instant.now().toEpochMilli(),
        )
    }
}
