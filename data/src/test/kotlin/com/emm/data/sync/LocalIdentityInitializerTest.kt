package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalIdentityInitializerTest {

    private lateinit var db: HelloDb
    private lateinit var subject: DefaultLocalIdentityInitializer

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        subject = DefaultLocalIdentityInitializer(
            db = db,
            localDeviceIdentityProvider = LocalDeviceIdentityProvider(db),
        )
    }

    @Test
    fun `ensureReady reuses existing account state when app account already exists`() = runTest {
        seedDeviceIdentity(deviceId = "device-1")
        db.localFirstQueries.upsertLocalAccountState(
            appAccountId = "remote-account-1",
            authUserId = "auth-1",
            pairingState = "Paired",
            createdAt = 10L,
            updatedAt = 10L,
        )

        val state = subject.ensureReady()

        assertEquals("device-1", state.deviceId)
        assertEquals("remote-account-1", state.appAccountId)
        assertEquals("Paired", state.pairingState)
        assertFalse(state.createdLocalAccount)
        assertEquals("auth-1", db.localFirstQueries.selectLocalAccountState().executeAsOne().authUserId)
    }

    @Test
    fun `ensureReady creates stable synthetic account when local account is missing`() = runTest {
        seedDeviceIdentity(deviceId = "device-7")

        val first = subject.ensureReady()
        val second = subject.ensureReady()

        assertEquals("local-only:device-7", first.appAccountId)
        assertEquals("local-only:device-7", second.appAccountId)
        assertEquals("LocalOnly", first.pairingState)
        assertFalse(second.createdLocalAccount)
    }

    @Test
    fun `ensureReady preserves outbox and checkpoint when creating local-only account`() = runTest {
        seedDeviceIdentity(deviceId = "device-9")
        val appAccountId = "local-only:device-9"
        db.localFirstQueries.upsertSyncCheckpoint(
            appAccountId = appAccountId,
            lastPulledCursor = 33L,
            lastSuccessfulSyncAt = 44L,
            lastSyncError = "timeout",
            lastSyncErrorAt = 45L,
            updatedAt = 46L,
        )
        db.localFirstQueries.insertOperation(
            opId = "op-1",
            appAccountId = appAccountId,
            entityType = "deck",
            entityId = "deck-1",
            operationType = "Create",
            payload = "{}",
            lamport = 1L,
            originDeviceId = "device-9",
            createdAt = 1L,
            status = "Pending",
            retryCount = 0L,
            lastAttemptAt = null,
            lastError = null,
        )

        subject.ensureReady()

        val checkpoint = db.localFirstQueries.selectSyncCheckpoint(appAccountId).executeAsOne()
        val pending = db.localFirstQueries.countRetryableOperations(appAccountId, DrainOutbox.MAX_RETRY_COUNT).executeAsOne()
        assertEquals(33L, checkpoint.lastPulledCursor)
        assertEquals(44L, checkpoint.lastSuccessfulSyncAt)
        assertEquals(1L, pending)
    }

    private fun seedDeviceIdentity(deviceId: String) {
        db.localFirstQueries.upsertLocalDeviceIdentity(
            deviceId = deviceId,
            installId = "install-$deviceId",
            lamportCounter = 0L,
            createdAt = 1L,
            updatedAt = 1L,
        )
    }
}
