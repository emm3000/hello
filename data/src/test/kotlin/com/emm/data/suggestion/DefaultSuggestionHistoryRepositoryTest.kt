package com.emm.data.suggestion

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.domain.suggestion.SuggestionHistory
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DefaultSuggestionHistoryRepositoryTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultSuggestionHistoryRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultSuggestionHistoryRepository(db = db, ioDispatcher = Dispatchers.IO)
    }

    @Test
    fun `fetch splits the history by kind with the most recent first`() = runTest {
        db.suggestionQueries.insertHistory(kind = "SITUATION", text = "At a cafe", shownAt = 1_000L)
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "order", shownAt = 1_000L)
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "table", shownAt = 1_000L)
        db.suggestionQueries.insertHistory(kind = "SITUATION", text = "At the airport", shownAt = 2_000L)
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "gate", shownAt = 2_000L)

        val history: SuggestionHistory = subject.fetch()

        assertEquals(
            SuggestionHistory(
                situations = listOf("At the airport", "At a cafe"),
                words = listOf("gate", "table", "order"),
            ),
            history,
        )
    }

    @Test
    fun `deleteShownBefore removes only the entries shown before the cutoff`() = runTest {
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "older", shownAt = 999L)
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "boundary", shownAt = 1_000L)
        db.suggestionQueries.insertHistory(kind = "ITEM", text = "newer", shownAt = 1_001L)

        subject.deleteShownBefore(Instant.ofEpochMilli(1_000L))

        assertEquals(listOf("newer", "boundary"), subject.fetch().words)
    }
}
