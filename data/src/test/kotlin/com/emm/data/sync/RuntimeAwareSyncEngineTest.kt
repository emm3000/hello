package com.emm.data.sync

import com.emm.domain.sync.SyncState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeAwareSyncEngineTest {

    @Test
    fun `local-only runOnce does not delegate and keeps neutral state`() = runTest {
        val delegate = mockk<DefaultSyncEngine>(relaxed = true)
        val subject = RuntimeAwareSyncEngine(
            delegate = delegate,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false),
        )

        subject.runOnce()

        coVerify(exactly = 0) { delegate.runOnce() }
        assertEquals(SyncState(), subject.state.value)
    }

    @Test
    fun `remote runOnce delegates to underlying engine`() = runTest {
        val delegate = mockk<DefaultSyncEngine>(relaxed = true)
        val subject = RuntimeAwareSyncEngine(
            delegate = delegate,
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = true),
        )
        coEvery { delegate.runOnce() } returns Unit

        subject.runOnce()

        coVerify(exactly = 1) { delegate.runOnce() }
    }

    private class FakeSyncRuntimePolicy(
        override val remoteEnabled: Boolean,
    ) : SyncRuntimePolicy {
        override val modeLabel: String = if (remoteEnabled) "remote" else "local-only"
    }
}
