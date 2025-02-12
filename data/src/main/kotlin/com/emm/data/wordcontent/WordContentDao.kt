package com.emm.data.wordcontent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wordContent: WordContentEntity)

    @Query("SELECT * from word_content")
    fun select(): Flow<List<WordContentEntity>>

    @Upsert
    suspend fun upsert(words: List<WordContentEntity>)

    @Transaction
    @Query("SELECT * from word_content WHERE wordId = :wordId")
    suspend fun fetchContentWord(wordId: String): List<WordContentWithExamples>
}