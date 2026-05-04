package com.emm.hello.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.emm.data.sync.SyncRuntimePolicy
import com.emm.domain.sync.SyncEngine
import com.emm.domain.sync.SyncState
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class SyncEngineWorkerTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `local-only worker exits before invoking remote sync`() = runTest {
        val syncEngine = FakeSyncEngine()
        stopKoin()
        startKoin {
            modules(
                module {
                    single<SyncEngine> { syncEngine }
                    single<SyncRuntimePolicy> { FakeSyncRuntimePolicy(remoteEnabled = false) }
                }
            )
        }
        val subject = SyncEngineWorker(
            appContext = mockk<Context>(relaxed = true),
            workerParameters = mockk<WorkerParameters>(relaxed = true),
        )

        val result = subject.doWork()

        assertThat(syncEngine.runOnceCalls).isEqualTo(0)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    @Test
    fun `local-only worker ignores failing remote engine because it never calls it`() = runTest {
        val syncEngine = FakeSyncEngine(shouldThrow = true)
        stopKoin()
        startKoin {
            modules(
                module {
                    single<SyncEngine> { syncEngine }
                    single<SyncRuntimePolicy> { FakeSyncRuntimePolicy(remoteEnabled = false) }
                }
            )
        }
        val subject = SyncEngineWorker(
            appContext = mockk<Context>(relaxed = true),
            workerParameters = mockk<WorkerParameters>(relaxed = true),
        )

        val result = subject.doWork()

        assertThat(syncEngine.runOnceCalls).isEqualTo(0)
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    }

    private class FakeSyncEngine(
        private val shouldThrow: Boolean = false,
    ) : SyncEngine {
        var runOnceCalls: Int = 0
        override val state = MutableStateFlow(SyncState())

        override suspend fun runOnce() {
            runOnceCalls += 1
            if (shouldThrow) error("boom")
        }
    }

    private class FakeSyncRuntimePolicy(
        override val remoteEnabled: Boolean,
    ) : SyncRuntimePolicy {
        override val modeLabel: String = if (remoteEnabled) "remote" else "local-only"
    }
}
