package com.emm.data.scrap

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert

@Dao
interface ExampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(example: ExampleEntity)

    @Upsert
    suspend fun upsert(words: List<ExampleEntity>)
}