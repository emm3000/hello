package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExportImportIntegrationTest {

    private lateinit var db: HelloDb
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)

        db.localFirstQueries.upsertLocalDeviceIdentity(
            deviceId = "test-device",
            installId = "install-test",
            lamportCounter = 0L,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }

    private fun createExportDataSource(): Pair<ExportBackupDataSource, ByteArrayOutputStream> {
        val baos = ByteArrayOutputStream()
        val resolver = mockk<ContentResolver>(relaxed = true)
        every { resolver.openOutputStream(any()) } returns baos
        contentResolver = resolver
        return Pair(ExportBackupDataSource(db, resolver), baos)
    }

    private fun createImportDataSource(): ImportBackupDataSource {
        val resolver = mockk<ContentResolver>(relaxed = true)
        contentResolver = resolver
        return ImportBackupDataSource(db, resolver)
    }

    private fun provideInputStream(content: String) {
        val bais = ByteArrayInputStream(content.toByteArray())
        every { contentResolver.openInputStream(any()) } returns bais
    }

    private fun countDecks(): Int = db.deckQueries.all().executeAsList().size
    private fun countFlashcards(): Int = db.flashcardQueries.all().executeAsList().size

    @Test
    fun `export then import produces identical counts`() = runTest {
        seedData()

        val (exportDS, baos) = createExportDataSource()
        val importDS = createImportDataSource()

        val beforeDeckCount = countDecks()
        val beforeCardCount = countFlashcards()
        assertEquals(2, beforeDeckCount)
        assertEquals(3, beforeCardCount)

        val exportUri = Uri.parse("content://test/export-backup.json")
        exportDS.export(exportUri)
        val exportedJson = baos.toString()
        assertTrue(exportedJson.contains("Spanish"))
        assertTrue(exportedJson.contains("French"))

        clearAllTables()

        assertEquals(0, countDecks())
        assertEquals(0, countFlashcards())

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(exportedJson)
        val result = importDS.import(importUri)
        assertTrue(result.isSuccess)

        assertEquals(beforeDeckCount, countDecks())
        assertEquals(beforeCardCount, countFlashcards())
    }

    @Test
    fun `imported data has correct content after round-trip`() = runTest {
        db.deckQueries.insert(
            id = "deck-spanish",
            name = "Spanish Vocab",
            description = "Common Spanish words",
            createdAt = 100L,
            updatedAt = 200L,
            deletedAt = null,
        )
        db.flashcardQueries.create(
            id = "card-hola",
            deckId = "deck-spanish",
            word = "hola",
            meaning = "hello",
            translation = null,
            phonetic = null,
            partOfSpeech = "interjection",
            type = null,
            note = null,
            register = null,
            levelBand = null,
            domain = null,
            lemma = null,
            whyUseful = null,
            usagePattern = null,
            irregularFormsJson = null,
            collocationsJson = null,
            commonMistake = null,
            confusableWithJson = null,
            clozeSentence = null,
            sourceContext = null,
            warningsJson = null,
            studyCardsJson = null,
            qualityChecksJson = null,
            createdAt = 300L,
            updatedAt = 400L,
            deletedAt = null,
        )

        val (exportDS, baos) = createExportDataSource()
        val importDS = createImportDataSource()

        val exportUri = Uri.parse("content://test/spanish-export.json")
        exportDS.export(exportUri)
        val exportedJson = baos.toString()

        clearAllTables()

        val importUri = Uri.parse("content://test/spanish-import.json")
        provideInputStream(exportedJson)
        importDS.import(importUri)

        val decks = db.deckQueries.all().executeAsList()
        assertEquals(1, decks.size)
        assertEquals("Spanish Vocab", decks[0].name)

        val cards = db.flashcardQueries.all().executeAsList()
        assertEquals(1, cards.size)
        assertEquals("hola", cards[0].word)
        assertEquals("hello", cards[0].meaning)
    }

    @Test
    fun `export empty database produces valid parseable envelope`() = runTest {
        val (exportDS, baos) = createExportDataSource()

        val exportUri = Uri.parse("content://test/empty-export.json")
        val result = exportDS.export(exportUri)

        assertTrue(result.isSuccess)

        val json = baos.toString()
        val envelope = Json { ignoreUnknownKeys = true }
            .decodeFromString(BackupEnvelope.serializer(), json)
        assertEquals(0, envelope.decks.size)
        assertEquals(0, envelope.flashcards.size)
    }

    @Test
    fun `soft-deleted records are not exported`() = runTest {
        val (exportDS, baos) = createExportDataSource()

        db.deckQueries.insert(
            id = "deck-active",
            name = "Active Deck",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        db.deckQueries.insert(
            id = "deck-deleted",
            name = "Deleted Deck",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = 999L,
        )

        val exportUri = Uri.parse("content://test/soft-delete-export.json")
        exportDS.export(exportUri)

        val json = baos.toString()
        assertTrue(json.contains("Active Deck"))
        assertTrue(!json.contains("Deleted Deck"))
    }

    @Test
    fun `import with review projections restores projections correctly`() = runTest {
        val (exportDS, baos) = createExportDataSource()
        val importDS = createImportDataSource()

        db.deckQueries.insert(
            id = "deck-review",
            name = "Review Deck",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        db.flashcardQueries.create(
            id = "card-review",
            deckId = "deck-review",
            word = "word",
            meaning = "meaning",
            translation = null,
            phonetic = null,
            partOfSpeech = null,
            type = null,
            note = null,
            register = null,
            levelBand = null,
            domain = null,
            lemma = null,
            whyUseful = null,
            usagePattern = null,
            irregularFormsJson = null,
            collocationsJson = null,
            commonMistake = null,
            confusableWithJson = null,
            clozeSentence = null,
            sourceContext = null,
            warningsJson = null,
            studyCardsJson = null,
            qualityChecksJson = null,
            createdAt = 200L,
            updatedAt = 200L,
            deletedAt = null,
        )
        db.localFirstQueries.upsertReviewProjection(
            flashcardId = "card-review",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            sourceEventId = "event-1",
            updatedAt = 2000L,
        )

        val exportUri = Uri.parse("content://test/review-export.json")
        exportDS.export(exportUri)
        val exportedJson = baos.toString()

        db.exportQueries.deleteAllFlashcardExamples()
        db.exportQueries.deleteAllFlashcards()
        db.exportQueries.deleteAllDecks()
        db.exportQueries.deleteAllReviewProjections()

        val importUri = Uri.parse("content://test/review-import.json")
        provideInputStream(exportedJson)
        importDS.import(importUri)

        val projections = db.localFirstQueries.allReviewProjections().executeAsList()
        assertEquals(1, projections.size)
        assertEquals("card-review", projections[0].flashcardId)
        assertEquals(2000L, projections[0].nextReviewAt)
        assertEquals(2.5, projections[0].easeFactor, 0.001)
    }

    @Test
    fun `import malformed JSON does not corrupt database`() = runTest {
        db.deckQueries.insert(
            id = "existing-deck",
            name = "Existing",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        assertEquals(1, countDecks())

        val malformedJson = """{schemaVersion": 1, "decks": [invalid"""
        val importDS = createImportDataSource()
        val importUri = Uri.parse("content://test/malformed.json")
        provideInputStream(malformedJson)

        val result = importDS.import(importUri)
        assertTrue(result.isFailure)

        assertEquals(1, countDecks())
    }

    @Test
    fun `import truncated JSON does not corrupt database`() = runTest {
        db.deckQueries.insert(
            id = "existing-deck",
            name = "Existing",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        assertEquals(1, countDecks())

        val truncatedJson = """{"schemaVersion": 1, "exportedAt": 123,"""
        val importDS = createImportDataSource()
        val importUri = Uri.parse("content://test/truncated.json")
        provideInputStream(truncatedJson)

        val result = importDS.import(importUri)
        assertTrue(result.isFailure)

        assertEquals(1, countDecks())
    }

    @Test
    fun `transaction rollback preserves database on mid-import failure`() = runTest {
        db.deckQueries.insert(
            id = "existing-deck",
            name = "Survivor Deck",
            description = "Should survive a failed import",
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        assertEquals(1, countDecks())

        val result = runCatching {
            db.transaction {
                db.exportQueries.deleteAllDecks()
                error("simulated mid-import failure")
            }
        }

        assertTrue(result.isFailure)
        assertEquals(1, countDecks())
        assertEquals(
            "Survivor Deck",
            db.deckQueries.findById("existing-deck").executeAsOne().name,
        )
    }

    @Suppress("LongMethod")
    private fun seedData() {
        db.deckQueries.insert(
            id = "deck-1",
            name = "Spanish",
            description = "Spanish vocab",
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        db.deckQueries.insert(
            id = "deck-2",
            name = "French",
            description = "French vocab",
            createdAt = 200L,
            updatedAt = 200L,
            deletedAt = null,
        )

        listOf("card-1", "card-2", "card-3").forEachIndexed { index, cardId ->
            db.flashcardQueries.create(
                id = cardId,
                deckId = if (index < 2) "deck-1" else "deck-2",
                word = "word$index",
                meaning = "meaning$index",
                translation = null,
                phonetic = null,
                partOfSpeech = null,
                type = null,
                note = null,
                register = null,
                levelBand = null,
                domain = null,
                lemma = null,
                whyUseful = null,
                usagePattern = null,
                irregularFormsJson = null,
                collocationsJson = null,
                commonMistake = null,
                confusableWithJson = null,
                clozeSentence = null,
                sourceContext = null,
                warningsJson = null,
                studyCardsJson = null,
                qualityChecksJson = null,
                createdAt = (300L + index),
                updatedAt = (300L + index),
                deletedAt = null,
            )
        }

        listOf("spanish", "french", "travel", "food", "business")
            .forEachIndexed { index, tagName ->
                db.exportQueries.insertTag(
                    id = "tag-$index",
                    name = tagName,
                    createdAt = (400L + index),
                    deletedAt = null,
                )
            }

        db.tagQueries.insertDeckTag(
            tagId = "tag-0",
            deckId = "deck-1",
            createdAt = 500L,
        )
        db.tagQueries.insertDeckTag(
            tagId = "tag-1",
            deckId = "deck-1",
            createdAt = 500L,
        )
        db.tagQueries.insertDeckTag(
            tagId = "tag-2",
            deckId = "deck-2",
            createdAt = 500L,
        )

        db.localFirstQueries.upsertReviewProjection(
            flashcardId = "card-1",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
            sourceEventId = "event-1",
            updatedAt = 2000L,
        )
    }

    private fun clearAllTables() {
        db.exportQueries.deleteAllFlashcardExamples()
        db.exportQueries.deleteAllFlashcards()
        db.exportQueries.deleteAllDecks()
        db.exportQueries.deleteAllTags()
        db.exportQueries.deleteAllReviewEvents()
        db.exportQueries.deleteAllReviewProjections()
    }
}
