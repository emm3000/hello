package com.emm.data.word

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("word")
data class WordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val hasContent: Boolean,
    val createdAt: Long,
)