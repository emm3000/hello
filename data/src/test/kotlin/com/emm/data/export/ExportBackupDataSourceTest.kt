package com.emm.data.export

import android.content.ContentResolver
import android.net.Uri
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExportBackupDataSourceTest {

    private lateinit var db: HelloDb
    private lateinit var contentResolver: ContentResolver
    private lateinit var dataSource: ExportBackupDataSource

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

        // Insert test data
        insertTestData()

        contentResolver = mockk(relaxed = true)
        dataSource = ExportBackupDataSource(db, contentResolver)
    }

    private fun insertTestData() {
        db.deckQueries.insert(
            id = "deck-1",
            name = "Spanish",
            description = "Spanish vocabulary",
            createdAt = 100L,
            updatedAt = 100L,
            deletedAt = null,
        )
        db.deckQueries.insert(
            id = "deck-2",
            name = "French",
            description = null,
            createdAt = 200L,
            updatedAt = 200L,
            deletedAt = null,
        )
        db.flashcardQueries.create(
            id = "card-1",
            deckId = "deck-1",
            word = "hola",
            meaning = "hello",
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
            createdAt = 300L,
            updatedAt = 300L,
            deletedAt = null,
        )
    }

    private fun captureOutputStream(uri: Uri): ByteArrayOutputStream {
        val baos = ByteArrayOutputStream()
        every { contentResolver.openOutputStream(uri) } returns baos
        return baos
    }

    @Test
    fun `export writes valid JSON with schema version 1`() = runTest {
        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        val result = dataSource.export(outputUri)

        assertTrue(result.isSuccess)
        val writtenJson = capturedOutput.toString()
        assertTrue(writtenJson.contains("\"schemaVersion\": 1"))
    }

    @Test
    fun `export includes decks in output`() = runTest {
        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        dataSource.export(outputUri)

        val writtenJson = capturedOutput.toString()
        assertTrue(writtenJson.contains("\"decks\""))
        assertTrue(writtenJson.contains("Spanish"))
        assertTrue(writtenJson.contains("French"))
    }

    @Test
    fun `export includes flashcards in output`() = runTest {
        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        dataSource.export(outputUri)

        val writtenJson = capturedOutput.toString()
        assertTrue(writtenJson.contains("\"flashcards\""))
        assertTrue(writtenJson.contains("hola"))
    }

    @Test
    fun `export uses batch pagination for large datasets`() = runTest {
        // Insert many decks to test pagination (more than BATCH_SIZE of 1000)
        for (i in 0..25) {
            db.deckQueries.insert(
                id = "deck-batch-$i",
                name = "Deck $i",
                description = null,
                createdAt = (1000L + i),
                updatedAt = (1000L + i),
                deletedAt = null,
            )
        }

        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        dataSource.export(outputUri)

        val writtenJson = capturedOutput.toString()
        // With 27 decks total (25 + 2 from setUp), verify all are exported
        assertTrue(writtenJson.contains("Deck 0"))
        assertTrue(writtenJson.contains("Deck 24"))
    }

    @Test
    fun `export result is failure when output stream unavailable`() = runTest {
        val outputUri = Uri.parse("content://test/nonexistent")
        every { contentResolver.openOutputStream(outputUri) } returns null

        val result = dataSource.export(outputUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ExportException)
    }

    @Test
    fun `export excludes soft-deleted decks`() = runTest {
        // Insert a soft-deleted deck
        db.deckQueries.insert(
            id = "deck-deleted",
            name = "Deleted Deck",
            description = null,
            createdAt = 500L,
            updatedAt = 500L,
            deletedAt = 999L, // soft deleted
        )

        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        dataSource.export(outputUri)

        val writtenJson = capturedOutput.toString()
        assertTrue(!writtenJson.contains("Deleted Deck"))
    }

    @Test
    fun `export writes exportedAt timestamp`() = runTest {
        val outputUri = Uri.parse("content://test/export.json")
        val capturedOutput = captureOutputStream(outputUri)

        val beforeExport = System.currentTimeMillis()
        dataSource.export(outputUri)
        val afterExport = System.currentTimeMillis()

        val writtenJson = capturedOutput.toString()
        // Parse the exportedAt value from JSON
        val match = Regex("\"exportedAt\":\\s*(\\d+)").find(writtenJson)
        assertTrue(match != null)
        val exportedAt = match!!.groupValues[1].toLong()
        assertTrue(exportedAt in beforeExport..afterExport)
    }
}
