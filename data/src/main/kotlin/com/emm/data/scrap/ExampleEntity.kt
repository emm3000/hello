package com.emm.data.scrap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("word_example")
data class ExampleEntity(
    @PrimaryKey val id: String,
    val number: String,
    val title: String,
    val sentences: String, // list
    val contentId: String,
)