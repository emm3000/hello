package com.emm.data.migration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import org.junit.Assert.assertEquals
import org.junit.Test

class DeadSyncSchemaMigrationTest {

    @Test
    fun `the sync tables are gone after migration`() {
        val driver: JdbcSqliteDriver = createSchema3Driver()

        HelloDb.Schema.migrate(driver = driver, oldVersion = 3L, newVersion = HelloDb.Schema.version)

        assertEquals(emptyList<String>(), readTableNames(driver))
    }

    @Test
    fun `the device identity survives migration`() {
        val driver: JdbcSqliteDriver = createSchema3Driver()
        insertDeviceIdentity(driver, deviceId = "device-1")

        HelloDb.Schema.migrate(driver = driver, oldVersion = 3L, newVersion = HelloDb.Schema.version)

        assertEquals(listOf("device-1"), readDeviceIds(driver))
    }

    private fun createSchema3Driver(): JdbcSqliteDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SCHEMA_3_SYNC_TABLES.forEach { driver.execute(null, it, 0) }
        return driver
    }

    private fun insertDeviceIdentity(driver: JdbcSqliteDriver, deviceId: String) {
        driver.execute(
            identifier = null,
            sql = "INSERT INTO LocalDeviceIdentity " +
                "(singletonId, deviceId, installId, lamportCounter, createdAt, updatedAt) " +
                "VALUES (1, ?, ?, 0, 1, 1)",
            parameters = 2,
        ) {
            bindString(0, deviceId)
            bindString(1, "install-1")
        }
    }

    private fun readDeviceIds(driver: JdbcSqliteDriver): List<String> =
        readColumn(driver, "SELECT deviceId FROM LocalDeviceIdentity")

    private fun readTableNames(driver: JdbcSqliteDriver): List<String> = readColumn(
        driver,
        "SELECT name FROM sqlite_master WHERE type = 'table' " +
            "AND name IN ('LocalAccountState', 'OperationLog', 'SyncCheckpoint', 'AppliedRemoteOperation') " +
            "ORDER BY name",
    )

    private fun readColumn(driver: JdbcSqliteDriver, sql: String): List<String> {
        val values = mutableListOf<String>()
        driver.executeQuery(
            identifier = null,
            sql = sql,
            parameters = 0,
            mapper = { cursor ->
                while (cursor.next().value) {
                    values += cursor.getString(0).orEmpty()
                }
                QueryResult.Unit
            },
        )
        return values
    }

    private companion object {
        val SCHEMA_3_SYNC_TABLES: List<String> = listOf(
            """
                CREATE TABLE LocalDeviceIdentity (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    deviceId TEXT NOT NULL UNIQUE,
                    installId TEXT NOT NULL UNIQUE,
                    lamportCounter INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent(),
            """
                CREATE TABLE LocalAccountState (
                    singletonId INTEGER NOT NULL PRIMARY KEY CHECK (singletonId = 1),
                    appAccountId TEXT,
                    authUserId TEXT,
                    pairingState TEXT NOT NULL DEFAULT 'Unpaired',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent(),
            """
                CREATE TABLE OperationLog (
                    opId TEXT NOT NULL PRIMARY KEY,
                    appAccountId TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operationType TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    lamport INTEGER NOT NULL,
                    originDeviceId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    retryCount INTEGER NOT NULL DEFAULT 0,
                    lastAttemptAt INTEGER,
                    lastError TEXT
                )
            """.trimIndent(),
            """
                CREATE TABLE SyncCheckpoint (
                    appAccountId TEXT NOT NULL PRIMARY KEY,
                    lastPulledCursor INTEGER NOT NULL DEFAULT 0,
                    lastSuccessfulSyncAt INTEGER,
                    lastSyncError TEXT,
                    lastSyncErrorAt INTEGER,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent(),
            """
                CREATE TABLE AppliedRemoteOperation (
                    appAccountId TEXT NOT NULL,
                    opId TEXT NOT NULL,
                    cursor INTEGER NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operationType TEXT NOT NULL,
                    processedAt INTEGER NOT NULL,
                    PRIMARY KEY (appAccountId, opId)
                )
            """.trimIndent(),
        )
    }
}
