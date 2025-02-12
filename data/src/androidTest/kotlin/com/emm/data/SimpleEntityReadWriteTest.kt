package com.emm.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emm.data.wordcontent.ExampleDao
import com.emm.data.wordcontent.ExampleEntity
import com.emm.data.wordcontent.WordContentDao
import com.emm.data.wordcontent.WordContentEntity
import com.emm.data.word.WordDao
import com.emm.data.word.WordEntity
import com.emm.domain.word.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SimpleEntityReadWriteTest {
    private lateinit var wordDao: WordDao
    private lateinit var wordContentDao: WordContentDao
    private lateinit var exampleDao: ExampleDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        wordDao = db.wordDao()
        wordContentDao = db.wordContentDao()
        exampleDao = db.exampleDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeUserAndReadInList() = runBlocking {
        val uuid: () -> String = { UUID.randomUUID().toString() }
        val wordId = uuid.invoke()
        WordEntity(
            id = wordId,
            word = "iusto",
            hasContent = false,
            createdAt = 0L,
        ).also { wordDao.insert(it) }
        val first: List<WordEntity> = wordDao.all().first()

        val wordContentId = uuid.invoke()
        WordContentEntity(
            id = wordContentId,
            wordFromScrap = "sociosqu",
            sourceType = SourceType.IA.name,
            pos = "conubia",
            wordId = wordId
        ).also { wordContentDao.insert(it) }

        ExampleEntity(
            id = uuid.invoke(),
            number = "eros",
            title = "simul",
            sentences = "deserunt",
            contentId = wordContentId
        ).also { exampleDao.insert(it) }

        val first1: List<WordContentEntity> = wordContentDao.select().firstOrNull() ?: emptyList()
        val select = exampleDao.select().firstOrNull() ?: emptyList()
        assert(first.isNotEmpty())
        assert(select.isNotEmpty())
        assert(first1.isNotEmpty())
    }
}