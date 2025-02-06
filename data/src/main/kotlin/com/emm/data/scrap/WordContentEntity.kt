package com.emm.data.scrap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("word_content")
data class WordContentEntity(
    @PrimaryKey val id: String,
    val word: String,
    val pos: String,
    val wordId: String,
)