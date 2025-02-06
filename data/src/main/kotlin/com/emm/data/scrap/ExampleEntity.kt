package com.emm.data.scrap

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName ="word_example",
    foreignKeys = [
        ForeignKey(
            entity = WordContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
)
data class ExampleEntity(
    @PrimaryKey val id: String,
    val number: String,
    val title: String,
    val sentences: String, // list
    val contentId: String,
)