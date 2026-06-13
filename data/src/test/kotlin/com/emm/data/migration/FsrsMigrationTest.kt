package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * JVM integration test for the FSRS-6 schema migration (version 1 to current).
 *
 * Covers all spec scenarios from "Requirement: Schema Migration 1->2 Preserves nextReviewAt":
 *   (a) Every existing card's nextReviewAt is unchanged after migration.
 *   (b) A never-reviewed card (no ReviewProjection row) stays unaffected by the migration.
 *   (c) A card with interval=10 / easeFactor=2.5 gets non-zero seeded stability and difficulty.
 *   (d) The migration itself runs without error (implicit: test completes without exception).
 *
 * HOW IT WORKS
 * -----------
 * We create a schema-1 in-memory database by executing the v1 DDL directly, then call
 * HelloDb.Schema.migrate(driver, oldVersion=1, newVersion=HelloDb.Schema.version) which applies
 * the 1.sqm ALTER TABLE + UPDATE statements — exactly the path an existing version-1 install
 * takes when it upgrades.
 *
 * NOTE: This test runs entirely on the JVM (JdbcSqliteDriver / sqlite4java) without a device.
 */
@Suppress("MaxLineLength")
class FsrsMigrationTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates an in-memory database with only the schema-v1 ReviewEvent and ReviewProjection
     * tables (no FSRS-6 columns), matching the structure captured in 1.db.
     */
    private fun createSchema1Driver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
                CREATE TABLE ReviewEvent (
                    eventId TEXT NOT NULL,
                    flashcardId TEXT NOT NULL,
                    grade TEXT NOT NULL,
                    reviewedAt INTEGER NOT NULL,
                    nextReviewAt INTEGER NOT NULL,
                    easeFactor REAL NOT NULL,
                    interval INTEGER NOT NULL,
                    repetitions INTEGER NOT NULL,
                    lapses INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY (eventId)
                )
            """.trimIndent(),
            0,
        )
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
                    PRIMARY KEY (flashcardId)
                )
            """.trimIndent(),
            0,
        )
        return driver
    }

    private fun insertLegacyProjection(
        driver: JdbcSqliteDriver,
        flashcardId: String,
        lastReviewedAt: Long,
        nextReviewAt: Long,
        easeFactor: Double,
        interval: Long,
        repetitions: Long,
        lapses: Long,
    ) {
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO ReviewProjection
                    (flashcardId, lastReviewedAt, nextReviewAt, easeFactor, interval,
                     repetitions, lapses, sourceEventId, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = 9,
        ) {
            bindString(0, flashcardId)
            bindLong(1, lastReviewedAt)
            bindLong(2, nextReviewAt)
            bindDouble(3, easeFactor)
            bindLong(4, interval)
            bindLong(5, repetitions)
            bindLong(6, lapses)
            bindString(7, UUID.randomUUID().toString())
            bindLong(8, lastReviewedAt)
        }
    }

    /**
     * Runs the FSRS-6 migration on the given driver from version 1 — the real upgrade path
     * for an existing install (the shipped schema baseline is captured in 1.db).
     */
    private fun applyMigration(driver: JdbcSqliteDriver) {
        HelloDb.Schema.migrate(
            driver = driver,
            oldVersion = 1L,
            newVersion = HelloDb.Schema.version,
        )
    }

    private fun queryAllProjections(
        driver: JdbcSqliteDriver,
    ): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT flashcardId, nextReviewAt, state, stability, difficulty FROM ReviewProjection",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    result += mapOf(
                        "flashcardId" to cursor.getString(0),
                        "nextReviewAt" to cursor.getLong(1),
                        "state" to cursor.getString(2),
                        "stability" to cursor.getDouble(3),
                        "difficulty" to cursor.getDouble(4),
                    )
                }
                QueryResult.Unit
            },
        )
        return result
    }

    // ── spec scenarios ────────────────────────────────────────────────────────

    /**
     * Scenario: Existing card due dates unchanged after upgrade.
     *
     * Seeds three cards with distinct nextReviewAt values; after migration every nextReviewAt
     * must equal its pre-migration value.
     */
    @Test
    fun `nextReviewAt of every existing card is unchanged after migration`() {
        val driver = createSchema1Driver()

        val cards = listOf(
            Triple("card-a", 1_000_000L, 2_000_000L),
            Triple("card-b", 5_000_000L, 9_000_000L),
            Triple("card-c", 100L, 200L),
        )
        for ((id, last, next) in cards) {
            insertLegacyProjection(
                driver = driver,
                flashcardId = id,
                lastReviewedAt = last,
                nextReviewAt = next,
                easeFactor = 2.5,
                interval = 10L,
                repetitions = 5L,
                lapses = 0L,
            )
        }

        applyMigration(driver)

        val rows = queryAllProjections(driver)
        val byId = rows.associateBy { it["flashcardId"] as String }
        for ((id, _, next) in cards) {
            assertEquals(
                "nextReviewAt for $id must be unchanged after migration",
                next,
                byId.getValue(id)["nextReviewAt"],
            )
        }
    }

    /**
     * Scenario: Legacy card stability and difficulty seeded from interval/ease.
     *
     * A card with interval=10 / easeFactor=2.5 must have non-zero stability and difficulty
     * after migration.
     *
     * Expected values from the formula in 1.sqm:
     *   stability = MAX(10.0, 0.001) = 10.0
     *   difficulty = MIN(10.0, MAX(1.0, 10.0 - (2.5 - 1.3) * (9.0 / 1.4)))
     *             = MIN(10.0, MAX(1.0, 10.0 - 1.2 * 6.4286))
     *             = 10.0 - 7.7143 = 2.2857
     */
    @Test
    fun `legacy card with interval=10 and easeFactor=2_5 gets non-zero seeded stability and difficulty`() {
        val driver = createSchema1Driver()
        insertLegacyProjection(
            driver = driver,
            flashcardId = "card-seeded",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 10L,
            repetitions = 5L,
            lapses = 0L,
        )

        applyMigration(driver)

        val rows = queryAllProjections(driver)
        assertEquals(1, rows.size)
        val row = rows[0]

        val stability = row["stability"] as Double
        val difficulty = row["difficulty"] as Double

        assertTrue("stability must be > 0 for interval=10, got $stability", stability > 0.0)
        assertTrue("difficulty must be > 0 for easeFactor=2.5, got $difficulty", difficulty > 0.0)
        assertTrue("difficulty must be <= 10.0, got $difficulty", difficulty <= 10.0)
        assertTrue("difficulty must be >= 1.0, got $difficulty", difficulty >= 1.0)
        assertEquals("stability seeded from interval", 10.0, stability, 0.001)
        assertEquals("difficulty mapped from easeFactor=2.5", 2.286, difficulty, 0.01)
    }

    /**
     * Scenario: Card state is correctly seeded from interval and lapses.
     *
     *   interval <= 0               → 'NEW'
     *   interval = 1 AND lapses > 0 → 'RELEARNING'
     *   otherwise                   → 'REVIEW'
     */
    @Test
    fun `card state is correctly seeded from interval and lapses`() {
        val driver = createSchema1Driver()

        insertLegacyProjection(
            driver = driver,
            flashcardId = "card-new",
            lastReviewedAt = 0L,
            nextReviewAt = 0L,
            easeFactor = 2.5,
            interval = 0L,
            repetitions = 0L,
            lapses = 0L,
        )
        insertLegacyProjection(
            driver = driver,
            flashcardId = "card-relearning",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.2,
            interval = 1L,
            repetitions = 3L,
            lapses = 2L,
        )
        insertLegacyProjection(
            driver = driver,
            flashcardId = "card-review",
            lastReviewedAt = 1000L,
            nextReviewAt = 2000L,
            easeFactor = 2.5,
            interval = 10L,
            repetitions = 5L,
            lapses = 0L,
        )

        applyMigration(driver)

        val rows = queryAllProjections(driver).associateBy { it["flashcardId"] as String }
        assertEquals("NEW", rows.getValue("card-new")["state"])
        assertEquals("RELEARNING", rows.getValue("card-relearning")["state"])
        assertEquals("REVIEW", rows.getValue("card-review")["state"])
    }

    /**
     * Scenario: Legacy card graduated after one clean review (interval=1, lapses=0).
     *
     * This is the most common case in an SM-2 app with no learning steps: a card reviewed
     * once and passed is immediately REVIEW state with an interval of 1 day.
     *
     *   state     → 'REVIEW'  (interval > 0 and lapses = 0 → falls through to the ELSE branch)
     *   stability → MAX(1.0, 0.001) = 1.0
     *   difficulty → in [1.0, 10.0]  (formula applied to easeFactor)
     */
    @Test
    fun `card with interval=1 and lapses=0 is seeded as REVIEW with stability=1_0`() {
        val driver = createSchema1Driver()
        insertLegacyProjection(
            driver = driver,
            flashcardId = "card-clean-one",
            lastReviewedAt = 1000L,
            nextReviewAt = 86400000L,
            easeFactor = 2.5,
            interval = 1L,
            repetitions = 1L,
            lapses = 0L,
        )

        applyMigration(driver)

        val rows = queryAllProjections(driver)
        assertEquals(1, rows.size)
        val row = rows[0]

        assertEquals("REVIEW", row["state"])
        val stability = row["stability"] as Double
        assertEquals("stability seeded from interval=1", 1.0, stability, 0.001)
        val difficulty = row["difficulty"] as Double
        assertTrue("difficulty must be >= 1.0, got $difficulty", difficulty >= 1.0)
        assertTrue("difficulty must be <= 10.0, got $difficulty", difficulty <= 10.0)
    }

    /**
     * Scenario: Migration adds the rating column to ReviewEvent with default 0 for legacy rows.
     */
    @Test
    fun `ReviewEvent rating column is added with default 0 for legacy rows`() {
        val driver = createSchema1Driver()
        driver.execute(
            identifier = null,
            sql = """
                INSERT INTO ReviewEvent
                    (eventId, flashcardId, grade, reviewedAt, nextReviewAt,
                     easeFactor, interval, repetitions, lapses, createdAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = 10,
        ) {
            bindString(0, "event-legacy")
            bindString(1, "card-x")
            bindString(2, "review")
            bindLong(3, 1000L)
            bindLong(4, 2000L)
            bindDouble(5, 2.5)
            bindLong(6, 1L)
            bindLong(7, 1L)
            bindLong(8, 0L)
            bindLong(9, 1000L)
        }

        applyMigration(driver)

        val ratings = mutableListOf<Long>()
        driver.executeQuery(
            identifier = null,
            sql = "SELECT rating FROM ReviewEvent WHERE eventId = 'event-legacy'",
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    ratings += cursor.getLong(0) ?: -1L
                }
                QueryResult.Unit
            },
        )
        assertEquals(1, ratings.size)
        assertEquals(0L, ratings[0])
    }

    /**
     * Scenario: Migration does not fabricate ReviewProjection rows for never-reviewed cards.
     *
     * The migration only UPDATEs existing rows; cards with no ReviewProjection row remain
     * without one.  The due-card query treats NULL nextReviewAt as due-now (query-layer concern).
     */
    @Test
    fun `migration does not create ReviewProjection rows for never-reviewed cards`() {
        val driver = createSchema1Driver()
        // No rows seeded — simulates a card that has never been reviewed.

        applyMigration(driver)

        val rows = queryAllProjections(driver)
        assertEquals(
            "Migration must not fabricate ReviewProjection rows",
            0,
            rows.size,
        )
    }
}
