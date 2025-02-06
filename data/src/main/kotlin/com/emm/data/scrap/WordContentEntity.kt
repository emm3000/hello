package com.emm.data.scrap

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emm.data.word.WordEntity

@Entity(
    tableName = "word_content",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["wordId"], unique = true)]
)
data class WordContentEntity(
    @PrimaryKey val id: String,
    val wordFromScrap: String,
    val pos: String,
    val wordId: String,
)