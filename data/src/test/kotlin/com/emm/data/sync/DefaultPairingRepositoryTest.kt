package com.emm.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.remote.DataStore
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DefaultPairingRepositoryTest {

    private lateinit var db: HelloDb
    private lateinit var remote: SupabaseSyncRemoteDataSource
    private lateinit var dataStore: DataStore
    private lateinit var subject: DefaultPairingRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HelloDb.Schema.create(driver)
        db = HelloDb(driver)
        remote = mockk()
        dataStore = mockk()
        every { dataStore.clearDefaultDeck() } just runs

        subject = DefaultPairingRepository(
            remote = remote,
            localDeviceIdentityProvider = LocalDeviceIdentityProvider(db),
            db = db,
            dataStore = dataStore,
        )
    }

    @Test
    fun `redeemPairingCode clears sync scoped data when account changes`() = runTest {
        seedLocalAccount(accountId = "old-account")
        seedLocalData()
        every { dataStore.clearDefaultDeck() } just runs
        io.mockk.coEvery { remote.ensureAnonymousSession() } returns Unit
        io.mockk.coEvery { remote.redeemPairingCode(any(), any(), any(), any()) } returns PairingRedeemResponse(
            appAccountId = "new-account",
            appDeviceId = "device-1",
            authUserId = "auth-1",
            pairingSessionId = "session-1",
            joined = true,
        )

        subject.redeemPairingCode("123456")

        val accountState = db.localFirstQueries.selectLocalAccountState().executeAsOneOrNull()
        val checkpoint = db.localFirstQueries.selectSyncCheckpoint().executeAsOneOrNull()
        assertEquals("new-account", accountState?.appAccountId)
        assertEquals(0L, checkpoint?.lastPulledCursor)
        assertNull(db.deckQueries.findById("deck-1").executeAsOneOrNull())
        assertNull(db.quotesQueries.findById("quote-1").executeAsOneOrNull())
        assertEquals(
            0L,
            db.localFirstQueries
                .countRetryableOperations(maxRetries = DrainOutbox.MAX_RETRY_COUNT)
                .executeAsOne(),
        )
        assertNull(db.localFirstQueries.findProcessedRemoteOperation("remote-op-1").executeAsOneOrNull())
        verify(exactly = 1) { dataStore.clearDefaultDeck() }
    }

    @Test
    fun `redeemPairingCode keeps local data when account does not change`() = runTest {
        seedLocalAccount(accountId = "same-account")
        seedLocalData()
        io.mockk.coEvery { remote.ensureAnonymousSession() } returns Unit
        io.mockk.coEvery { remote.redeemPairingCode(any(), any(), any(), any()) } returns PairingRedeemResponse(
            appAccountId = "same-account",
            appDeviceId = "device-1",
            authUserId = "auth-1",
            pairingSessionId = "session-1",
            joined = true,
        )

        subject.redeemPairingCode("123456")

        assertNotNull(db.deckQueries.findById("deck-1").executeAsOneOrNull())
        assertNotNull(db.quotesQueries.findById("quote-1").executeAsOneOrNull())
        verify(exactly = 0) { dataStore.clearDefaultDeck() }
    }

    private fun seedLocalAccount(accountId: String) {
        db.localFirstQueries.upsertLocalAccountState(
            appAccountId = accountId,
            authUserId = "auth-old",
            pairingState = "Paired",
            createdAt = 1,
            updatedAt = 1,
        )
        db.localFirstQueries.upsertSyncCheckpoint(
            lastPulledCursor = 99,
            lastSuccessfulSyncAt = 100,
            lastSyncError = null,
            lastSyncErrorAt = null,
            updatedAt = 100,
        )
    }

    private fun seedLocalData() {
        db.deckQueries.insert(
            id = "deck-1",
            name = "Deck",
            description = "desc",
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 1,
        )
        db.quotesQueries.insert(
            id = "quote-1",
            title = "Title",
            phrase = "Phrase",
            description = "Desc",
            translation = "Tr",
            example = "Ex",
            context = "Ctx",
            pronunciation = "Pron",
            formality = "Formal",
            tags = "tag",
            category = "cat",
            createdAt = 1,
            updatedAt = 1,
            deletedAt = null,
            originDeviceId = "device-1",
            lastModifiedByDeviceId = "device-1",
            versionLamport = 1,
        )
        db.localFirstQueries.insertOperation(
            opId = "pending-op-1",
            entityType = "deck",
            entityId = "deck-1",
            operationType = "Create",
            payload = "{}",
            lamport = 1,
            originDeviceId = "device-1",
            createdAt = 1,
            status = "Pending",
            retryCount = 0,
            lastAttemptAt = null,
            lastError = null,
        )
        db.localFirstQueries.markRemoteOperationProcessed(
            opId = "remote-op-1",
            cursor = 1,
            entityType = "deck",
            entityId = "deck-1",
            operationType = "create",
            processedAt = 1,
        )
    }
}
