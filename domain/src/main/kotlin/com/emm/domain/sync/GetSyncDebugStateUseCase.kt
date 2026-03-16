package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

class GetSyncDebugStateUseCase(
    private val repository: SyncDebugStateRepository,
) {
    fun fetch(): Flow<SyncDebugState> {
        return repository.observe()
    }
}
