package com.emm.data.suggestion

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.emm.data.HelloDb
import com.emm.data.SuggestedWordCandidate
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DefaultWordSuggestionCacheTest {

    private val fixedInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000L)

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultWordSuggestionCache

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        db = HelloDb(driver)
        subject = DefaultWordSuggestionCache(
            db = db,
            clock = Clock { fixedInstant },
            ioDispatcher = Dispatchers.IO,
        )
    }

    @Test
    fun `observe emits null while no batch is stored`() = runTest {
        assertNull(subject.observe().first())
    }

    @Test
    fun `observe emits the stored situation and words in insertion order`() = runTest {
        subject.replace(
            WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
            ),
        )

        val stored: WordSuggestions? = subject.observe().first()

        assertEquals("At a cafe", stored?.situation)
        assertEquals(listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")), stored?.words)
    }

    @Test
    fun `replace swaps the whole batch and restarts the positions at zero`() = runTest {
        subject.replace(
            WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
            ),
        )

        subject.replace(
            WordSuggestions(situation = "At the airport", words = listOf(SuggestedWord("gate", "puerta"))),
        )

        val stored: WordSuggestions? = subject.observe().first()
        assertEquals("At the airport", stored?.situation)
        assertEquals(listOf(SuggestedWord("gate", "puerta")), stored?.words)

        val candidates: List<SuggestedWordCandidate> = db.suggestionQueries.allCandidates().executeAsList()
        assertEquals(listOf(0L), candidates.map { candidate -> candidate.position })
        assertEquals(listOf("gate"), candidates.map { candidate -> candidate.word })
    }

    @Test
    fun `replace stamps the batch with the clock instant`() = runTest {
        subject.replace(WordSuggestions(situation = "At a cafe", words = emptyList()))

        assertEquals(fixedInstant.toEpochMilli(), db.suggestionQueries.selectBatch().executeAsOne().createdAt)
    }

    @Test
    fun `observe never emits a batch crossed with the words of another batch`() = runTest {
        val atACafe = WordSuggestions(
            situation = "At a cafe",
            words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
        )
        val atTheAirport = WordSuggestions(
            situation = "At the airport",
            words = listOf(SuggestedWord("gate", "puerta"), SuggestedWord("boarding", "embarque")),
        )

        subject.observe().test {
            assertNull(awaitItem())

            subject.replace(atACafe)
            assertEquals(atACafe, awaitItem())

            subject.replace(atTheAirport)
            assertEquals(atTheAirport, awaitItem())

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `replace with no words still stores a batch`() = runTest {
        subject.replace(WordSuggestions(situation = "At a cafe", words = emptyList()))

        val stored: WordSuggestions? = subject.observe().first()

        assertEquals("At a cafe", stored?.situation)
        assertEquals(emptyList<SuggestedWord>(), stored?.words)
    }
}
