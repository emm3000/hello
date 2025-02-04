package com.emm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("word")
data class WordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val createdAt: String,
    val updatedAt: String,
)