package com.emm.data.scrap

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wordContent: WordContentEntity)

    @Query("SELECT * from word_content WHERE wordId = :wordId")
    suspend fun fetchContentWord(wordId: String): WordContentWithExamples?
}