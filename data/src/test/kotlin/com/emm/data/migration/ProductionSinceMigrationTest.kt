package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal const val PRE_PRODUCTION_SINCE_SCHEMA_VERSION: Long = 5L

class ProductionSinceMigrationTest {

    private fun createSchema5Driver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
                CREATE TABLE ReviewProjection (
                    flashcardId TEXT NOT NULL,
                    lastReviewedAt INTEGER NOT NULL,
                    nextReviewAt INTEGER NOT NULL,
                    easeFactor REAL NOT NULL,
                    interval INTEGER NOT NULL,
                    repetitions INTEGER NOT NULL,
                    lapses INTEGER NOT NULL,
                    sourceEventId TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    state TEXT NOT NULL DEFAULT 'NEW',
                    stability REAL NOT NULL DEFAULT 0.0,
                    difficulty REAL NOT NULL DEFAULT 0.0,
                    PRIMARY KEY (flashcardId)
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun insertPreMigrationProjection(
        driver: JdbcSqliteDriver,
        flashcardId: String,
        lastReviewedAt: Long,
        nextReviewAt: Long,
        easeFactor: Double = 2.5,
        interval: Long = 10L,
        repetitions: Long = 5L,
        lapses: Long = 0L,
        sourceEventId: String = "event",
        state: String = "REVIEW",
        stability: Double = 10.0,
        difficulty: Double = 3.0,
    ) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO ReviewProjection
                    (flashcardId, lastReviewedAt, nextReviewAt, easeFactor, interval,
                     repetitions, lapses, sourceEventId, updatedAt, state, stability, difficulty)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = 12,
        ) {
            bindString(0, flashcardId)
            bindLong(1, lastReviewedAt)
            bindLong(2, nextReviewAt)
            bindDouble(3, easeFactor)
            bindLong(4, interval)
            bindLong(5, repetitions)
            bindLong(6, lapses)
            bindString(7, sourceEventId)
            bindLong(8, lastReviewedAt)
            bindString(9, state)
            bindDouble(10, stability)
            bindDouble(11, difficulty)
        }
    }

    private fun applyMigration(driver: JdbcSqliteDriver) {
        HelloDb.Schema.migrate(
            driver = driver,
            oldVersion = PRE_PRODUCTION_SINCE_SCHEMA_VERSION,
            newVersion = HelloDb.Schema.version,
        )
    }

    private fun queryAllProjectionsWithProductionSince(
        driver: JdbcSqliteDriver,
    ): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = """
                SELECT flashcardId, lastReviewedAt, nextReviewAt, easeFactor, interval,
                       repetitions, lapses, sourceEventId, updatedAt, state, stability,
                       difficulty, productionSince
                FROM ReviewProjection
            """.trimIndent(),
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "flashcardId" to cursor.getString(0),
                        "lastReviewedAt" to cursor.getLong(1),
                        "nextReviewAt" to cursor.getLong(2),
                        "easeFactor" to cursor.getDouble(3),
                        "interval" to cursor.getLong(4),
                        "repetitions" to cursor.getLong(5),
                        "lapses" to cursor.getLong(6),
                        "sourceEventId" to cursor.getString(7),
                        "updatedAt" to cursor.getLong(8),
                        "state" to cursor.getString(9),
                        "stability" to cursor.getDouble(10),
                        "difficulty" to cursor.getDouble(11),
                        "productionSince" to cursor.getLong(12),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    @Test
    fun `productionSince column exists and is NULL for every legacy row after migration`() {
        val driver = createSchema5Driver()

        insertPreMigrationProjection(
            driver = driver,
            flashcardId = "card-a",
            lastReviewedAt = 1_000L,
            nextReviewAt = 2_000L,
            easeFactor = 2.5,
            interval = 10L,
            repetitions = 5L,
            lapses = 0L,
            sourceEventId = "event-a",
            state = "REVIEW",
            stability = 10.0,
            difficulty = 3.0,
        )
        insertPreMigrationProjection(
            driver = driver,
            flashcardId = "card-b",
            lastReviewedAt = 5_000L,
            nextReviewAt = 9_000L,
            easeFactor = 2.2,
            interval = 30L,
            repetitions = 8L,
            lapses = 1L,
            sourceEventId = "event-b",
            state = "REVIEW",
            stability = 30.0,
            difficulty = 4.5,
        )

        applyMigration(driver)

        val rows = queryAllProjectionsWithProductionSince(driver)
        assertEquals(2, rows.size)
        rows.forEach { row ->
            assertNull(
                "productionSince must be NULL for legacy row ${row["flashcardId"]}",
                row["productionSince"],
            )
        }
    }

    @Test
    fun `every pre-existing ReviewProjection column is unchanged after migration`() {
        val driver = createSchema5Driver()

        insertPreMigrationProjection(
            driver = driver,
            flashcardId = "card-unchanged",
            lastReviewedAt = 12_345L,
            nextReviewAt = 67_890L,
            easeFactor = 2.35,
            interval = 21L,
            repetitions = 6L,
            lapses = 2L,
            sourceEventId = "event-unchanged",
            state = "RELEARNING",
            stability = 21.0,
            difficulty = 6.5,
        )

        applyMigration(driver)

        val row = queryAllProjectionsWithProductionSince(driver).single()
        assertEquals("card-unchanged", row["flashcardId"])
        assertEquals(12_345L, row["lastReviewedAt"])
        assertEquals(67_890L, row["nextReviewAt"])
        assertEquals(2.35, row["easeFactor"] as Double, 0.0001)
        assertEquals(21L, row["interval"])
        assertEquals(6L, row["repetitions"])
        assertEquals(2L, row["lapses"])
        assertEquals("event-unchanged", row["sourceEventId"])
        assertEquals("RELEARNING", row["state"])
        assertEquals(21.0, row["stability"] as Double, 0.0001)
        assertEquals(6.5, row["difficulty"] as Double, 0.0001)
    }
}
