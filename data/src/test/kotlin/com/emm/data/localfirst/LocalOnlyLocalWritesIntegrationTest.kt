package com.emm.data.localfirst

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.deck.DefaultTagRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FsrsCard
import com.emm.domain.flashcard.FsrsState
import com.emm.domain.ids.toDeckId
import com.emm.domain.study.ReviewGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

class LocalOnlyLocalWritesIntegrationTest {

    private lateinit var db: HelloDb
    private lateinit var localDeviceIdentityProvider: LocalDeviceIdentityProvider
    private lateinit var localIdentityInitializer: DefaultLocalIdentityInitializer

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        localDeviceIdentityProvider = LocalDeviceIdentityProvider(db)
        localIdentityInitializer = DefaultLocalIdentityInitializer(
            db = db,
            localDeviceIdentityProvider = localDeviceIdentityProvider,
        )
    }

    @Test
    fun `local-only identity keeps deck flashcard and review writes fully local`() = runTest {
        seedDeviceIdentity(deviceId = "device-local-only")

        val identity = localIdentityInitializer.ensureReady()

        val tagRepository = DefaultTagRepository(db = db)
        val deckRepository = DefaultDeckRepository(db = db, tagRepository = tagRepository)
        val flashcardRepository = DefaultFlashcardRepository(
            db = db,
            json = Json,
            ioDispatcher = Dispatchers.IO,
        )
        val reviewRepository = DefaultFlashcardReviewRepository(db = db)

        deckRepository.create(CreateDeckInput(name = "Travel", description = "Trip phrases", tags = listOf("travel")))
        val deck = db.deckQueries.all().executeAsOne()

        val tags = db.tagQueries.findByDeckId(deck.id).executeAsList()

        assertEquals("device-local-only", identity.deviceId)
        assertFalse(identity.createdInstallation)
        assertEquals("Travel", deck.name)
        assertEquals(1, tags.size)
        assertEquals("travel", tags[0].name)

        val flashcardId = flashcardRepository.create(
            CreateFlashcardInput(
                deckId = deck.id.toDeckId(),
                word = "hello",
                meaning = "greeting",
                translation = "hola",
                phonetic = "/həˈloʊ/",
            )
        )

        val card = FsrsCard(
            flashcardId = flashcardId,
            state = FsrsState.REVIEW,
            stability = 1.0,
            difficulty = 5.0,
            lastReviewedAt = 200L,
            nextReviewAt = 300L,
            interval = 1L,
            reps = 1L,
            lapses = 0L,
        )
        reviewRepository.update(card, ReviewGrade.GOOD)

        val storedFlashcard = flashcardRepository.fetchById(flashcardId).flashcard
        val reviewProjection = db.localFirstQueries.findReviewProjectionByFlashcardId(flashcardId.value).executeAsOne()
        val reviewEvent = db.localFirstQueries.findReviewEventsByFlashcardId(flashcardId.value).executeAsOne()

        assertEquals("device-local-only", identity.deviceId)
        assertFalse(identity.createdInstallation)
        assertEquals("Travel", deck.name)
        assertEquals(flashcardId, storedFlashcard.id)
        assertEquals("hello", storedFlashcard.word)
        assertEquals("greeting", storedFlashcard.meaning)
        assertEquals(flashcardId.value, reviewProjection.flashcardId)
        assertEquals(300L, reviewProjection.nextReviewAt)
        assertEquals("REVIEW", reviewProjection.state)
        assertEquals(1.0, reviewProjection.stability, 0.0)
        assertEquals(5.0, reviewProjection.difficulty, 0.0)
        assertEquals(flashcardId.value, reviewEvent.flashcardId)
        // Real grade is persisted — the hardcoded "review" literal must not appear.
        assertEquals("GOOD", reviewEvent.grade)
        assertEquals(3L, reviewEvent.rating)
        assertNotNull(db.flashcardQueries.findById(flashcardId.value).executeAsOneOrNull())
    }

    @Test
    fun `deck creation with multiple tags persists tags and deck-tag relations`() = runTest {
        seedDeviceIdentity(deviceId = "device-tags")

        val identity = localIdentityInitializer.ensureReady()

        val tagRepository = DefaultTagRepository(db = db)
        val deckRepository = DefaultDeckRepository(db = db, tagRepository = tagRepository)

        deckRepository.create(
            CreateDeckInput(
                name = "Languages",
                description = "Learning phrases",
                tags = listOf("spanish", "  TRAVEL ", "French"),
            ),
        )
        val deck = db.deckQueries.all().executeAsOne()

        val tags = db.tagQueries.findByDeckId(deck.id).executeAsList()

        assertEquals("device-tags", identity.deviceId)
        assertFalse(identity.createdInstallation)
        assertEquals("Languages", deck.name)
        assertEquals(3, tags.size)
        val tagNames = tags.map { it.name }.sorted()
        assertEquals(listOf("french", "spanish", "travel"), tagNames)
    }

    @Test
    fun `local-only reopen loads previously persisted data without remote rehydration`() = runTest {
        val dbFile = Files.createTempFile("local-only-reopen", ".db")
        val jdbcUrl = "jdbc:sqlite:${dbFile.toAbsolutePath()}"
        try {
            val persistedFlashcardId = persistDataForReopenScenario(jdbcUrl)
            val reopenedData = loadReopenedData(jdbcUrl, persistedFlashcardId)

            assertEquals("device-reopen", reopenedData.deviceId)
            assertFalse(reopenedData.createdInstallation)
            assertEquals("Reopen deck", reopenedData.deckName)
            assertEquals("bye", reopenedData.flashcardWord)
            assertEquals(20L, reopenedData.nextReviewAt)
        } finally {
            Files.deleteIfExists(dbFile)
        }
    }

    private suspend fun persistDataForReopenScenario(jdbcUrl: String): com.emm.domain.ids.FlashcardId {
        val firstDriver = JdbcSqliteDriver(jdbcUrl)
        val firstDb = HelloDb(firstDriver)
        HelloDb.Schema.create(firstDriver)

        return try {
            val firstInitializer = DefaultLocalIdentityInitializer(
                db = firstDb,
                localDeviceIdentityProvider = LocalDeviceIdentityProvider(firstDb),
            )

            seedDeviceIdentity(firstDb, deviceId = "device-reopen")
            val identity = firstInitializer.ensureReady()

            val firstTagRepository = DefaultTagRepository(db = firstDb)
            val deckRepository = DefaultDeckRepository(db = firstDb, tagRepository = firstTagRepository)
            val flashcardRepository = DefaultFlashcardRepository(
                db = firstDb,
                json = Json,
                ioDispatcher = Dispatchers.IO,
            )
            val reviewRepository = DefaultFlashcardReviewRepository(db = firstDb)

            deckRepository.create(CreateDeckInput(name = "Reopen deck", description = "available after reopen"))
            assertFalse(identity.createdInstallation)
            val persistedDeck = firstDb.deckQueries.all().executeAsOne()
            val persistedFlashcardId = flashcardRepository.create(
                CreateFlashcardInput(
                    deckId = persistedDeck.id.toDeckId(),
                    word = "bye",
                    meaning = "farewell",
                    translation = "chau",
                    phonetic = "/baɪ/",
                )
            )
            reviewRepository.update(
                FsrsCard(
                    flashcardId = persistedFlashcardId,
                    state = FsrsState.REVIEW,
                    stability = 1.0,
                    difficulty = 5.0,
                    lastReviewedAt = 10L,
                    nextReviewAt = 20L,
                    interval = 1L,
                    reps = 1L,
                    lapses = 0L,
                ),
                ReviewGrade.GOOD,
            )
            persistedFlashcardId
        } finally {
            firstDriver.close()
        }
    }

    private suspend fun loadReopenedData(
        jdbcUrl: String,
        persistedFlashcardId: com.emm.domain.ids.FlashcardId,
    ): ReopenedData {
        val reopenedDriver = JdbcSqliteDriver(jdbcUrl)
        val reopenedDb = HelloDb(reopenedDriver)

        return try {
            val reopenedInitializer = DefaultLocalIdentityInitializer(
                db = reopenedDb,
                localDeviceIdentityProvider = LocalDeviceIdentityProvider(reopenedDb),
            )

            val reopenedIdentity = reopenedInitializer.ensureReady()
            val reopenedDeck = reopenedDb.deckQueries.all().executeAsOne()
            val reopenedFlashcard = reopenedDb.flashcardQueries
                .findById(persistedFlashcardId.value)
                .executeAsOne()
            val reopenedReview = reopenedDb.localFirstQueries
                .findReviewProjectionByFlashcardId(reopenedFlashcard.id)
                .executeAsOne()

            ReopenedData(
                deviceId = reopenedIdentity.deviceId,
                createdInstallation = reopenedIdentity.createdInstallation,
                deckName = reopenedDeck.name,
                flashcardWord = reopenedFlashcard.word,
                nextReviewAt = reopenedReview.nextReviewAt,
            )
        } finally {
            reopenedDriver.close()
        }
    }

    private data class ReopenedData(
        val deviceId: String,
        val createdInstallation: Boolean,
        val deckName: String,
        val flashcardWord: String,
        val nextReviewAt: Long,
    )

    private fun seedDeviceIdentity(db: HelloDb = this.db, deviceId: String) {
        db.localFirstQueries.upsertLocalDeviceIdentity(
            deviceId = deviceId,
            installId = "install-$deviceId",
            lamportCounter = 0L,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}
