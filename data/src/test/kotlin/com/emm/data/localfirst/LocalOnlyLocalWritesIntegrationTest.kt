package com.emm.data.localfirst

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.flashcard.GeminiService
import com.emm.domain.deck.CreateDeckInput
import com.emm.domain.flashcard.CreateFlashcardInput
import com.emm.domain.flashcard.FlashcardReview
import com.emm.domain.ids.toFlashcardId
import io.mockk.mockk
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

        val deckRepository = DefaultDeckRepository(db = db)
        val flashcardRepository = DefaultFlashcardRepository(
            db = db,
            geminiService = mockk<GeminiService>(),
            json = Json,
        )
        val reviewRepository = DefaultFlashcardReviewRepository(db = db)

        deckRepository.addDeck(CreateDeckInput(name = "Travel", description = "Trip phrases"))
        val deck = db.deckQueries.all().executeAsOne()

        val flashcardId = flashcardRepository.create(
            CreateFlashcardInput(
                deckId = deck.id,
                word = "hello",
                meaning = "greeting",
                translation = "hola",
                phonetic = "/həˈloʊ/",
            )
        )

        val review = FlashcardReview(
            flashcardId = flashcardId,
            lastReviewedAt = 200L,
            nextReviewAt = 300L,
            easeFactor = 2.6,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
        )
        reviewRepository.update(review)

        val storedFlashcard = flashcardRepository.fetchById(flashcardId.toFlashcardId())
        val reviewProjection = db.localFirstQueries.findReviewProjectionByFlashcardId(flashcardId).executeAsOne()
        val reviewEvent = db.localFirstQueries.findReviewEventsByFlashcardId(flashcardId).executeAsOne()

        assertEquals("device-local-only", identity.deviceId)
        assertFalse(identity.createdInstallation)
        assertEquals("Travel", deck.name)
        assertEquals(flashcardId, storedFlashcard.id)
        assertEquals("hello", storedFlashcard.word)
        assertEquals("greeting", storedFlashcard.meaning)
        assertEquals(flashcardId, reviewProjection.flashcardId)
        assertEquals(300L, reviewProjection.nextReviewAt)
        assertEquals(flashcardId, reviewEvent.flashcardId)
        assertNotNull(db.flashcardQueries.findById(flashcardId).executeAsOneOrNull())
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

    private suspend fun persistDataForReopenScenario(jdbcUrl: String): String {
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

            val deckRepository = DefaultDeckRepository(db = firstDb)
            val flashcardRepository = DefaultFlashcardRepository(
                db = firstDb,
                geminiService = mockk<GeminiService>(),
                json = Json,
            )
            val reviewRepository = DefaultFlashcardReviewRepository(db = firstDb)

            deckRepository.addDeck(CreateDeckInput(name = "Reopen deck", description = "available after reopen"))
            assertFalse(identity.createdInstallation)
            val persistedDeck = firstDb.deckQueries.all().executeAsOne()
            val persistedFlashcardId = flashcardRepository.create(
                CreateFlashcardInput(
                    deckId = persistedDeck.id,
                    word = "bye",
                    meaning = "farewell",
                    translation = "chau",
                    phonetic = "/baɪ/",
                )
            )
            reviewRepository.update(
                FlashcardReview(
                    flashcardId = persistedFlashcardId,
                    lastReviewedAt = 10L,
                    nextReviewAt = 20L,
                    easeFactor = 2.5,
                    interval = 1L,
                    repetitions = 1L,
                    lapses = 0L,
                )
            )
            persistedFlashcardId
        } finally {
            firstDriver.close()
        }
    }

    private suspend fun loadReopenedData(jdbcUrl: String, persistedFlashcardId: String): ReopenedData {
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
                .findById(persistedFlashcardId)
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
