package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class ImportBackupDataSourceTest {

    private lateinit var db: HelloDb
    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: ImportBackupDataSource

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)

        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)

        // Seed identity so deck insert works
        db.localFirstQueries.upsertLocalDeviceIdentity(
            deviceId = "test-device",
            installId = "install-test",
            lamportCounter = 0L,
            createdAt = 1L,
            updatedAt = 1L,
        )

        contentResolver = mockk(relaxed = true)
        dataSource = ImportBackupDataSource(db, contentResolver)
    }

    private fun provideInputStream(uri: Uri, content: String) {
        val bais = ByteArrayInputStream(content.toByteArray())
        every { contentResolver.openInputStream(uri) } returns bais
    }

    @Test
    fun `import successful restores all data from backup envelope`() = runTest {
        // First export some data
        val deckId = "deck-import-test"
        db.deckQueries.insert(
            id = deckId,
            name = "Original Deck",
            description = "Test description",
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        db.flashcardQueries.create(
            id = "card-import-test",
            deckId = deckId,
            word = "test-word",
            meaning = "test-meaning",
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
            enrichmentStatus = "ENRICHED",
            createdAt = 200L,
            updatedAt = 200L,
            deletedAt = null,
        )

        val originalDeckCount = db.deckQueries.all().executeAsList().size
        assertEquals(1, originalDeckCount)

        // Import a new backup
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [
                {"id": "deck-new", "name": "Imported Deck", "description": null, "createdAt": 300, "updatedAt": 300, "deletedAt": null}
              ],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        val result = dataSource.import(importUri)

        assertTrue(result.isSuccess)
        val decks = db.deckQueries.all().executeAsList()
        assertEquals(1, decks.size)
        assertEquals("Imported Deck", decks[0].name)
    }

    @Test
    fun `import returns failure with IncompatibleSchemaException for wrong schema version`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 99,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        val result = dataSource.import(importUri)
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IncompatibleSchemaException)
        assertTrue(exception!!.message!!.contains("99"))
        assertTrue(exception.message!!.contains("1"))
        assertTrue(exception.message!!.contains("2"))
    }

    @Test
    fun `import accepts schema version 2`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 2,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        val result = dataSource.import(importUri)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `import of schema v2 backup preserves real FSRS fields verbatim`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 2,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [
                {"eventId": "event-1", "flashcardId": "card-1", "grade": "GOOD", "reviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 10, "repetitions": 1, "lapses": 0, "createdAt": 1000, "rating": 3}
              ],
              "reviewProjections": [
                {"flashcardId": "card-1", "lastReviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 10, "repetitions": 1, "lapses": 0, "sourceEventId": "event-1", "updatedAt": 2000, "state": "REVIEW", "stability": 12.5, "difficulty": 4.2}
              ]
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        val event = db.localFirstQueries.findReviewEventsByFlashcardId("card-1").executeAsOne()
        assertEquals(3L, event.rating)

        val projection = db.localFirstQueries.findReviewProjectionByFlashcardId("card-1").executeAsOne()
        assertEquals("REVIEW", projection.state)
        assertEquals(12.5, projection.stability, 0.0001)
        assertEquals(4.2, projection.difficulty, 0.0001)
    }

    @Test
    fun `import of schema v1 backup seeds FSRS fields from legacy SM-2 columns`() = runTest {
        // interval=10, easeFactor=2.5 -> stability=10.0, difficulty~=2.2857, state=REVIEW
        // (same seeding formula as the 1.sqm on-device migration).
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [
                {"eventId": "event-1", "flashcardId": "card-1", "grade": "review", "reviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 10, "repetitions": 5, "lapses": 0, "createdAt": 1000}
              ],
              "reviewProjections": [
                {"flashcardId": "card-1", "lastReviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 10, "repetitions": 5, "lapses": 0, "sourceEventId": "event-1", "updatedAt": 2000}
              ]
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        val result = dataSource.import(importUri)
        assertTrue(result.isSuccess)

        val projection = db.localFirstQueries.findReviewProjectionByFlashcardId("card-1").executeAsOne()
        assertEquals("REVIEW", projection.state)
        assertEquals(10.0, projection.stability, 0.001)
        assertEquals(2.286, projection.difficulty, 0.01)
        // nextReviewAt must survive untouched, exactly like the on-device migration.
        assertEquals(2000L, projection.nextReviewAt)

        // v1 backups have no rating column; the legacy/unknown default (0) applies.
        val event = db.localFirstQueries.findReviewEventsByFlashcardId("card-1").executeAsOne()
        assertEquals(0L, event.rating)
    }

    @Test
    fun `import of schema v1 backup seeds NEW state for a never-graduated card`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": [
                {"flashcardId": "card-new", "lastReviewedAt": 0, "nextReviewAt": 0, "easeFactor": 2.5, "interval": 0, "repetitions": 0, "lapses": 0, "sourceEventId": "event-0", "updatedAt": 0}
              ]
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        val projection = db.localFirstQueries.findReviewProjectionByFlashcardId("card-new").executeAsOne()
        assertEquals("NEW", projection.state)
    }

    @Test
    fun `import result is failure when input stream unavailable`() = runTest {
        val importUri = Uri.parse("content://test/nonexistent")
        every { contentResolver.openInputStream(importUri) } returns null

        val result = dataSource.import(importUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ImportException)
    }

    @Test
    fun `import clears existing data before restoring`() = runTest {
        // Insert existing data
        db.deckQueries.insert(
            id = "existing-deck",
            name = "Existing Deck",
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        assertEquals(1, db.deckQueries.all().executeAsList().size)

        // Import empty backup
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        assertEquals(0, db.deckQueries.all().executeAsList().size)
    }

    @Test
    fun `import restores tags and deckTags correctly`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [
                {"id": "deck-with-tags", "name": "Tagged Deck", "description": null, "createdAt": 100, "updatedAt": 100, "deletedAt": null}
              ],
              "flashcards": [],
              "examples": [],
              "tags": [
                {"id": "tag-1", "name": "spanish", "createdAt": 200, "deletedAt": null},
                {"id": "tag-2", "name": "travel", "createdAt": 200, "deletedAt": null}
              ],
              "deckTags": [
                {"tagId": "tag-1", "deckId": "deck-with-tags", "createdAt": 300},
                {"tagId": "tag-2", "deckId": "deck-with-tags", "createdAt": 300}
              ],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        val tags = db.tagQueries.findByDeckId("deck-with-tags").executeAsList()
        assertEquals(2, tags.size)
        val tagNames = tags.map { it.name }.sorted()
        assertEquals(listOf("spanish", "travel"), tagNames)
    }

    @Test
    fun `import restores review events and projections correctly`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 1,
              "exportedAt": 1234567890,
              "decks": [],
              "flashcards": [],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [
                {"eventId": "event-1", "flashcardId": "card-1", "grade": "GOOD", "reviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 1, "repetitions": 1, "lapses": 0, "createdAt": 1000}
              ],
              "reviewProjections": [
                {"flashcardId": "card-1", "lastReviewedAt": 1000, "nextReviewAt": 2000, "easeFactor": 2.5, "interval": 1, "repetitions": 1, "lapses": 0, "sourceEventId": "event-1", "updatedAt": 2000}
              ]
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        val projections = db.localFirstQueries.allReviewProjections().executeAsList()
        assertEquals(1, projections.size)
        assertEquals("card-1", projections[0].flashcardId)
        assertEquals(2000L, projections[0].nextReviewAt)
    }

    @Test
    fun `import normalizes an unknown enrichment status so the card stays studiable`() = runTest {
        val backupJson = """
            {
              "schemaVersion": 2,
              "exportedAt": 1234567890,
              "decks": [
                {"id": "deck-1", "name": "Deck", "description": null, "createdAt": 100, "updatedAt": 100, "deletedAt": null}
              ],
              "flashcards": [
                {"id": "card-1", "deckId": "deck-1", "word": "borrow", "meaning": "to take and return", "translation": null, "phonetic": null, "partOfSpeech": null, "type": null, "note": null, "register": null, "levelBand": null, "domain": null, "lemma": null, "whyUseful": null, "usagePattern": null, "irregularFormsJson": null, "collocationsJson": null, "commonMistake": null, "confusableWithJson": null, "clozeSentence": null, "sourceContext": null, "warningsJson": null, "studyCardsJson": null, "qualityChecksJson": null, "enrichmentStatus": "enriched", "createdAt": 200, "updatedAt": 200, "deletedAt": null}
              ],
              "examples": [],
              "tags": [],
              "deckTags": [],
              "reviewEvents": [],
              "reviewProjections": []
            }
        """.trimIndent()

        val importUri = Uri.parse("content://test/import.json")
        provideInputStream(importUri, backupJson)

        dataSource.import(importUri)

        assertEquals(1L, db.flashcardQueries.countDueFlashcards(now = 1_000_000L).executeAsOne())
    }
}
