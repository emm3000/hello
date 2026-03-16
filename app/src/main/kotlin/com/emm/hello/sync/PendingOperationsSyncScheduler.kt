package com.emm.hello.sync

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.emm.data.HelloDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PendingOperationsSyncScheduler(
    private val appContext: Context,
    private val db: HelloDb,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var started: Boolean = false

    fun start() {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
        }

        scope.launch {
            db.localFirstQueries.countPendingOperations()
                .asFlow()
                .mapToOne(Dispatchers.IO)
                .map { count -> count > 0 }
                .distinctUntilChanged()
                .filter { hasPending -> hasPending }
                .collect {
                    Sync.requestImmediate(context = appContext)
                }
        }
    }
}
