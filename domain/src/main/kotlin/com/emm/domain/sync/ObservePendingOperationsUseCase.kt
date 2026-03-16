package com.emm.domain.sync

import kotlinx.coroutines.flow.Flow

class ObservePendingOperationsUseCase(
    private val repository: PendingOperationsRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.observeHasPendingOperations()
    }
}
