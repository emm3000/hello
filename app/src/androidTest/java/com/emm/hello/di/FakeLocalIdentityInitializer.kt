package com.emm.hello.di

import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.localfirst.LocalIdentityState

/**
 * Fake [LocalIdentityInitializer] that completes immediately during tests,
 * so [AppStartupCoordinator] reaches the Ready state without disk I/O.
 */
class FakeLocalIdentityInitializer : LocalIdentityInitializer {

    override suspend fun ensureReady(): LocalIdentityState =
        LocalIdentityState(
            deviceId = "test-device-id",
            createdInstallation = true,
        )
}
