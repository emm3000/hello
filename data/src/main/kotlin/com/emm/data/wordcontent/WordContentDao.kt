package com.emm.data.wordcontent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface WordContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wordContent: WordContentEntity)

    @Query("SELECT * from word_content")
    suspend fun select(): List<WordContentEntity>

    @Upsert
    suspend fun upsert(words: List<WordContentEntity>)

    @Transaction
    @Query("SELECT * from word_content WHERE wordId = :wordId")
    suspend fun fetchContentWord(wordId: String): List<WordContentWithExamples>
}