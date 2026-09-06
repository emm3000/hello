package com.emm.hello.startup

import com.emm.domain.authoring.FindPendingEnrichmentsUseCase
import com.emm.domain.flashcard.EnrichmentBacklog
import com.emm.domain.flashcard.EnrichmentStatus
import com.emm.domain.flashcard.FlashcardEnrichmentRepository
import com.emm.domain.ids.FlashcardId
import com.emm.domain.ids.toFlashcardId
import com.emm.domain.localfirst.LocalIdentityInitializer
import com.emm.domain.localfirst.LocalIdentityState
import com.emm.domain.onboarding.OnboardingStateRepository
import com.emm.domain.seed.SeedDataInitializer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupCoordinatorTest {

    @Test
    fun `startup prepares local identity and reports ready with hasSeenWelcome false`() = runTest {
        val localIdentityInitializer = FakeLocalIdentityInitializer()
        val seedDataInitializer = FakeSeedDataInitializer()
        val onboardingRepo = FakeOnboardingStateRepository(welcomeSeen = false)
        val subject = AppStartupCoordinator(
            localIdentityInitializer = localIdentityInitializer,
            seedDataInitializer = seedDataInitializer,
            onboardingStateRepository = onboardingRepo,
            findPendingEnrichments = FindPendingEnrichmentsUseCase(FakeEnrichmentRepository(pending = emptyList())),
            requeueEnrichments = {},
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(seedDataInitializer.calls).isEqualTo(1)
        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready(hasSeenWelcome = false))
    }

    @Test
    fun `startup ready state carries hasSeenWelcome true when flag is set`() = runTest {
        val localIdentityInitializer = FakeLocalIdentityInitializer()
        val onboardingRepo = FakeOnboardingStateRepository(welcomeSeen = true)
        val subject = AppStartupCoordinator(
            localIdentityInitializer = localIdentityInitializer,
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = onboardingRepo,
            findPendingEnrichments = FindPendingEnrichmentsUseCase(FakeEnrichmentRepository(pending = emptyList())),
            requeueEnrichments = {},
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready(hasSeenWelcome = true))
    }

    @Test
    fun `startup failure exposes local-only error message`() = runTest {
        val localIdentityInitializer = FakeLocalIdentityInitializer(shouldFail = true)
        val subject = AppStartupCoordinator(
            localIdentityInitializer = localIdentityInitializer,
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(FakeEnrichmentRepository(pending = emptyList())),
            requeueEnrichments = {},
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(localIdentityInitializer.calls).isEqualTo(1)
        assertThat(subject.state.value).isEqualTo(
            AppStartupState.Error("Couldn't prepare the app's local mode.")
        )
    }

    @Test
    fun `startup requeues the pending enrichments once ready`() = runTest {
        val requeuedBatches: MutableList<List<FlashcardId>> = mutableListOf()
        val subject = AppStartupCoordinator(
            localIdentityInitializer = FakeLocalIdentityInitializer(),
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(
                FakeEnrichmentRepository(pending = listOf("card-1", "card-2")),
            ),
            requeueEnrichments = { ids -> requeuedBatches += ids },
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(requeuedBatches).containsExactly(listOf("card-1".toFlashcardId(), "card-2".toFlashcardId()))
        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready(hasSeenWelcome = false))
    }

    @Test
    fun `startup requeues nothing when nothing is pending`() = runTest {
        val requeuedBatches: MutableList<List<FlashcardId>> = mutableListOf()
        val subject = AppStartupCoordinator(
            localIdentityInitializer = FakeLocalIdentityInitializer(),
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(FakeEnrichmentRepository(pending = emptyList())),
            requeueEnrichments = { ids -> requeuedBatches += ids },
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(requeuedBatches).isEmpty()
    }

    @Test
    fun `a lookup failure does not block ready`() = runTest {
        val requeuedBatches: MutableList<List<FlashcardId>> = mutableListOf()
        val subject = AppStartupCoordinator(
            localIdentityInitializer = FakeLocalIdentityInitializer(),
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(
                FakeEnrichmentRepository(pending = listOf("card-1"), shouldFail = true),
            ),
            requeueEnrichments = { ids -> requeuedBatches += ids },
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready(hasSeenWelcome = false))
        assertThat(requeuedBatches).isEmpty()
    }

    @Test
    fun `a scheduler failure does not block ready`() = runTest {
        val subject = AppStartupCoordinator(
            localIdentityInitializer = FakeLocalIdentityInitializer(),
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(
                FakeEnrichmentRepository(pending = listOf("card-1")),
            ),
            requeueEnrichments = { error("scheduler down") },
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(subject.state.value).isEqualTo(AppStartupState.Ready(hasSeenWelcome = false))
    }

    @Test
    fun `startup failure does not requeue`() = runTest {
        val requeuedBatches: MutableList<List<FlashcardId>> = mutableListOf()
        val subject = AppStartupCoordinator(
            localIdentityInitializer = FakeLocalIdentityInitializer(shouldFail = true),
            seedDataInitializer = FakeSeedDataInitializer(),
            onboardingStateRepository = FakeOnboardingStateRepository(),
            findPendingEnrichments = FindPendingEnrichmentsUseCase(
                FakeEnrichmentRepository(pending = listOf("card-1")),
            ),
            requeueEnrichments = { ids -> requeuedBatches += ids },
            scope = this,
        )

        subject.start()
        advanceUntilIdle()

        assertThat(subject.state.value).isEqualTo(
            AppStartupState.Error("Couldn't prepare the app's local mode.")
        )
        assertThat(requeuedBatches).isEmpty()
    }

    private class FakeLocalIdentityInitializer(private val shouldFail: Boolean = false) : LocalIdentityInitializer {
        var calls: Int = 0

        override suspend fun ensureReady(): LocalIdentityState {
            calls += 1
            if (shouldFail) error("boom")
            return LocalIdentityState(
                deviceId = "device-1",
                createdInstallation = true,
            )
        }
    }

    private class FakeSeedDataInitializer : SeedDataInitializer {
        var calls: Int = 0

        override suspend fun ensureSeeded() {
            calls += 1
        }
    }

    private class FakeOnboardingStateRepository(
        private val welcomeSeen: Boolean = false,
    ) : OnboardingStateRepository {
        override fun hasSeenWelcome(): Boolean = welcomeSeen
        override fun markWelcomeSeen() = Unit
    }

    private class FakeEnrichmentRepository(
        private val pending: List<String>,
        private val shouldFail: Boolean = false,
    ) : FlashcardEnrichmentRepository {

        override fun observeBacklog(): Flow<EnrichmentBacklog> = flowOf(EnrichmentBacklog())

        override suspend fun findIdsByStatus(status: EnrichmentStatus): List<FlashcardId> {
            if (shouldFail) error("boom")
            return pending.map(String::toFlashcardId)
        }

        override suspend fun markPending(ids: List<FlashcardId>) = Unit
    }
}
