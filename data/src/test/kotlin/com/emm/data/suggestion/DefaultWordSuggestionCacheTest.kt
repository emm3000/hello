package com.emm.data.suggestion

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.emm.data.HelloDb
import com.emm.data.SuggestedWordCandidate
import com.emm.data.SuggestionHistory
import com.emm.domain.suggestion.SuggestedWord
import com.emm.domain.suggestion.WordSuggestions
import com.emm.domain.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DefaultWordSuggestionCacheTest {

    private val fixedInstant: Instant = Instant.ofEpochMilli(1_700_000_000_000L)

    private var now: Instant = fixedInstant
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HelloDb
    private lateinit var subject: DefaultWordSuggestionCache

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        db = HelloDb(driver)
        subject = DefaultWordSuggestionCache(
            db = db,
            clock = Clock { now },
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

    @Test
    fun `replace records the situation and every stored word in the history at the clock instant`() = runTest {
        subject.replace(
            WordSuggestions(
                situation = "At a cafe",
                words = listOf(SuggestedWord("order", "pedir"), SuggestedWord("table", "mesa")),
            ),
        )

        val history: List<SuggestionHistory> = db.suggestionQueries.selectHistory().executeAsList()
        assertEquals(
            setOf(
                SuggestionHistory(kind = "SITUATION", text = "At a cafe", shownAt = 1_700_000_000_000L),
                SuggestionHistory(kind = "ITEM", text = "order", shownAt = 1_700_000_000_000L),
                SuggestionHistory(kind = "ITEM", text = "table", shownAt = 1_700_000_000_000L),
            ),
            history.toSet(),
        )
        assertEquals(3, history.size)
    }

    @Test
    fun `a failing history write rolls back the batch it was stored with`() = runTest {
        subject.replace(WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))))
        driver.execute(
            null,
            "CREATE TRIGGER reject_history BEFORE INSERT ON SuggestionHistory WHEN NEW.text = 'gate' " +
                "BEGIN SELECT RAISE(ABORT, 'rejected'); END",
            0,
        )

        val failure: Throwable? = runCatching {
            subject.replace(
                WordSuggestions(situation = "At the airport", words = listOf(SuggestedWord("gate", "puerta"))),
            )
        }.exceptionOrNull()

        assertTrue(failure != null)
        assertEquals(
            WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))),
            subject.observe().first(),
        )
        assertEquals(
            listOf("order", "At a cafe"),
            db.suggestionQueries.selectHistory().executeAsList().map { row -> row.text },
        )
    }

    @Test
    fun `markSeen stamps the batch once and keeps the first value`() = runTest {
        subject.replace(WordSuggestions(situation = "At a cafe", words = listOf(SuggestedWord("order", "pedir"))))

        now = Instant.ofEpochMilli(1_700_000_060_000L)
        subject.markSeen()
        now = Instant.ofEpochMilli(1_700_000_120_000L)
        subject.markSeen()

        assertEquals(Instant.ofEpochMilli(1_700_000_060_000L), subject.observe().first()?.firstSeenAt)
    }
}
