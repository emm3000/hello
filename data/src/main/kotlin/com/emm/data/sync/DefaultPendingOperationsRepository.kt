package com.emm.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.HelloDb
import com.emm.domain.sync.PendingOperationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DefaultPendingOperationsRepository(
    private val db: HelloDb,
) : PendingOperationsRepository {

    override fun observeHasPendingOperations(): Flow<Boolean> {
        return db.localFirstQueries.countPendingOperations()
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { count -> count > 0 }
            .distinctUntilChanged()
    }
}
