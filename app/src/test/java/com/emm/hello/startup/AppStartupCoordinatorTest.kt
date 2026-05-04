package com.emm.hello.startup

import com.emm.data.localfirst.LocalIdentityInitializer
import com.emm.data.localfirst.LocalIdentityState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupCoordinatorTest {

    @Test
    fun `startup prepares local identity and reports ready`() = runTest {
        val localIdentityInitializer = FakeLocalIdentityInitializer()
        val subject = AppStartupCoordinator(
            localIdentityInitializer = localIdentityInitializer,
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready)
    }

    @Test
    fun `startup failure exposes local-only error message`() = runTest {
        val localIdentityInitializer = FakeLocalIdentityInitializer(shouldFail = true)
        val subject = AppStartupCoordinator(
            localIdentityInitializer = localIdentityInitializer,
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(subject.state.value).isEqualTo(
            AppStartupState.Error("No se pudo preparar el modo local de la app.")
        )
    }

    private class FakeLocalIdentityInitializer : LocalIdentityInitializer {
        constructor(shouldFail: Boolean = false) {
            this.shouldFail = shouldFail
        }

        var calls: Int = 0
        private var shouldFail: Boolean = false

        override suspend fun ensureReady(): LocalIdentityState {
            calls += 1
            if (shouldFail) error("boom")
            return LocalIdentityState(
                deviceId = "device-1",
                createdInstallation = true,
            )
        }
    }
}
