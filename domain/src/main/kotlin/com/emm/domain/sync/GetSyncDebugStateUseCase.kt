package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

class GetSyncDebugStateUseCase(
    private val repository: SyncDebugStateRepository,
) {
    operator fun invoke(): Flow<SyncDebugState> {
        return repository.observe()
    }
}
