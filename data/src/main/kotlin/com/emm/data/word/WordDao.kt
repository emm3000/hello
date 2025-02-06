package com.emm.data.word

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Query("SELECT * FROM word")
    fun all(): Flow<List<WordEntity>>

    @Query("SELECT * FROM word WHERE word LIKE '%' || :name || '%'")
    fun searchBy(name: String): Flow<List<WordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordEntity)

    @Upsert
    suspend fun upsert(words: List<WordEntity>)

    @Delete
    suspend fun delete(word: WordEntity)

    @Query("SELECT * FROM word WHERE id = :wordId")
    suspend fun selectBy(wordId: String): WordEntity?
}