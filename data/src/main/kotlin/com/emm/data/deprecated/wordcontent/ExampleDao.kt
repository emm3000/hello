package com.emm.data.deprecated.wordcontent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(example: ExampleEntity)

    @Query("SELECT * FROM word_example")
    fun select(): Flow<List<ExampleEntity>>

    @Upsert
    suspend fun upsert(words: List<ExampleEntity>)
}