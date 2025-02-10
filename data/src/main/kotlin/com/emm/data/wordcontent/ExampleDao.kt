package com.emm.data.wordcontent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(example: ExampleEntity)

    @Query("SELECT * FROM word_example")
    suspend fun select(): List<ExampleEntity>

    @Upsert
    suspend fun upsert(words: List<ExampleEntity>)
}