package com.emm.data.deprecated.wordcontent

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emm.data.deprecated.word.WordEntity
import kotlinx.serialization.Serializable

@Serializable
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
    indices = [Index(value = ["wordId"])]
)
data class WordContentEntity(
    @PrimaryKey val id: String,
    val wordFromScrap: String,
    val pos: String,
    val sourceType: String,
    val wordId: String,
)