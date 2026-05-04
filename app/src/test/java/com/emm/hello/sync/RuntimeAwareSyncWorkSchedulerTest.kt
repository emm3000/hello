package com.emm.hello.sync

import android.content.Context
import com.emm.data.sync.SyncRuntimePolicy
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class RuntimeAwareSyncWorkSchedulerTest {

    @Test
    fun `local-only requestImmediate does not enqueue work`() {
        val gateway = FakeSyncGateway()
        val subject = RuntimeAwareSyncWorkScheduler(
            appContext = mockk<Context>(relaxed = true),
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false),
            syncGateway = gateway,
        )

        subject.requestImmediate()

        assertThat(gateway.requestImmediateCalls).isEqualTo(0)
    }

    @Test
    fun `local-only initialize does not register connectivity callback`() {
        val gateway = FakeSyncGateway()
        val subject = RuntimeAwareSyncWorkScheduler(
            appContext = mockk<Context>(relaxed = true),
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = false),
            syncGateway = gateway,
        )

        subject.initialize()

        assertThat(gateway.initializeCalls).isEqualTo(0)
        assertThat(gateway.onConnectivityAvailable).isNull()
    }

    @Test
    fun `remote initialize registers callback that requests immediate sync`() {
        val gateway = FakeSyncGateway()
        val subject = RuntimeAwareSyncWorkScheduler(
            appContext = mockk<Context>(relaxed = true),
            syncRuntimePolicy = FakeSyncRuntimePolicy(remoteEnabled = true),
            syncGateway = gateway,
        )

        subject.initialize()
        gateway.onConnectivityAvailable?.invoke()

        assertThat(gateway.initializeCalls).isEqualTo(1)
        assertThat(gateway.requestImmediateCalls).isEqualTo(1)
    }

    private class FakeSyncGateway : SyncGateway {
        var initializeCalls: Int = 0
        var requestImmediateCalls: Int = 0
        var shutdownCalls: Int = 0
        var onConnectivityAvailable: (() -> Unit)? = null

        override fun initialize(context: Context, onConnectivityAvailable: () -> Unit) {
            initializeCalls += 1
            this.onConnectivityAvailable = onConnectivityAvailable
        }

        override fun requestImmediate(context: Context) {
            requestImmediateCalls += 1
        }

        override fun shutdown(context: Context) {
            shutdownCalls += 1
        }
    }

    private class FakeSyncRuntimePolicy(
        override val remoteEnabled: Boolean,
    ) : SyncRuntimePolicy {
        override val modeLabel: String = if (remoteEnabled) "remote" else "local-only"
    }
}
