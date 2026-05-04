package com.emm.data.localfirst

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.HelloDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun `ensureReady reuses existing device identity without creating installation again`() = runTest {
        seedDeviceIdentity(deviceId = "device-1")

        val state = subject.ensureReady()

        assertEquals("device-1", state.deviceId)
        assertEquals(false, state.createdInstallation)
    }

    @Test
    fun `ensureReady creates stable device identity on first launch and reuses it later`() = runTest {
        val first = subject.ensureReady()
        val second = subject.ensureReady()

        assertEquals(true, first.deviceId.isNotBlank())
        assertEquals(first.deviceId, second.deviceId)
        assertEquals(true, first.createdInstallation)
        assertEquals(false, second.createdInstallation)
    }

    @Test
    fun `ensureReady only manages installation identity`() = runTest {
        seedDeviceIdentity(deviceId = "device-9")

        val state = subject.ensureReady()

        assertEquals("device-9", state.deviceId)
        assertEquals(0L, db.localFirstQueries.currentLamport().executeAsOne())
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
