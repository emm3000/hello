package com.emm.hello.sync

import android.content.Context
import com.emm.domain.sync.ObservePendingOperationsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class PendingOperationsSyncScheduler(
    private val appContext: Context,
    private val observePendingOperationsUseCase: ObservePendingOperationsUseCase,
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
            observePendingOperationsUseCase()
                .filter { hasPending -> hasPending }
                .collect {
                    Sync.requestImmediate(context = appContext)
                }
        }
    }
}
