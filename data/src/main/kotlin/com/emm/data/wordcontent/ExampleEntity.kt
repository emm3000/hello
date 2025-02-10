package com.emm.data.wordcontent

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_example",
    foreignKeys = [
        ForeignKey(
            entity = WordContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contentId"])]
)
data class ExampleEntity(
    @PrimaryKey val id: String,
    val number: String,
    val title: String,
    val sentences: String, // list
    val contentId: String,
)