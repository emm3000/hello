package com.emm.data.deck

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeckSearchQueryTest {
    private lateinit var db: HelloDb

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
    }

    @Test
    fun `observeFiltered matches query case-insensitive`() {
        seedDecks()

        val results = db.deckQueries.observeFiltered(
            query = "bio",
            selectedTagCount = 0,
            normalizedTagNames = emptyList(),
        ).executeAsList()

        assertEquals(listOf("deck-bio", "deck-biology-exam"), results.map { it.id })
    }

    @Test
    fun `observeFiltered returns empty when no matches`() {
        seedDecks()

        val results = db.deckQueries.observeFiltered(
            query = "history",
            selectedTagCount = 0,
            normalizedTagNames = emptyList(),
        ).executeAsList()

        assertEquals(emptyList<String>(), results.map { it.id })
    }

    @Test
    fun `observeFiltered filters by single tag`() {
        seedDecks()
        seedTags()

        val results = db.deckQueries.observeFiltered(
            query = "",
            selectedTagCount = 1,
            normalizedTagNames = listOf("exam"),
        ).executeAsList()

        assertEquals(listOf("deck-biology-exam", "deck-chem-exam"), results.map { it.id })
    }

    @Test
    fun `observeFiltered filters by all selected tags`() {
        seedDecks()
        seedTags()

        val results = db.deckQueries.observeFiltered(
            query = "",
            selectedTagCount = 2,
            normalizedTagNames = listOf("exam", "biology"),
        ).executeAsList()

        assertEquals(listOf("deck-biology-exam"), results.map { it.id })
    }

    @Test
    fun `observeFiltered applies query and tags as intersection`() {
        seedDecks()
        seedTags()

        val results = db.deckQueries.observeFiltered(
            query = "bio",
            selectedTagCount = 1,
            normalizedTagNames = listOf("exam"),
        ).executeAsList()

        assertEquals(listOf("deck-biology-exam"), results.map { it.id })
    }

    private fun seedDecks() {
        insertDeck(id = "deck-bio", name = "Bio Basics", createdAt = 10)
        insertDeck(id = "deck-biology-exam", name = "Biology Final", createdAt = 9)
        insertDeck(id = "deck-chem-exam", name = "Chemistry Exam", createdAt = 8)
    }

    private fun seedTags() {
        val examTagId = insertTag(name = "exam")
        val biologyTagId = insertTag(name = "biology")

        insertDeckTag(tagId = examTagId, deckId = "deck-biology-exam")
        insertDeckTag(tagId = biologyTagId, deckId = "deck-biology-exam")
        insertDeckTag(tagId = examTagId, deckId = "deck-chem-exam")
        insertDeckTag(tagId = biologyTagId, deckId = "deck-bio")
    }

    private fun insertDeck(id: String, name: String, createdAt: Long) {
        db.deckQueries.insert(
            id = id,
            name = name,
            description = null,
            createdAt = createdAt,
            updatedAt = createdAt,
            deletedAt = null,
        )
    }

    private fun insertTag(name: String): String {
        db.tagQueries.getOrCreateTags(
            name = name,
            createdAt = 1,
        )
        return db.tagQueries.findByName(name).executeAsOne().id
    }

    private fun insertDeckTag(tagId: String, deckId: String) {
        db.tagQueries.insertDeckTag(
            tagId = tagId,
            deckId = deckId,
            createdAt = 1,
        )
    }
}
