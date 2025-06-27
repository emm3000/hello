package com.emm.data.deprecated.word

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity("word")
data class WordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val hasContent: Boolean,
    val createdAt: Long,
)