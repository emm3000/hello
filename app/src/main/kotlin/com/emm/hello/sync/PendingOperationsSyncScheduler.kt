package com.emm.hello.sync

import android.content.Context
import android.util.Log
import com.emm.domain.sync.ObservePendingOperationsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class PendingOperationsSyncScheduler(
    private val appContext: Context,
    private val observePendingOperationsUseCase: ObservePendingOperationsUseCase,
) {

    @Volatile private var started: Boolean = false
    private var job: Job? = null

    fun start() {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
            job = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                observePendingOperationsUseCase()
                    .filter { hasPending -> hasPending }
                    .catch { error -> Log.e(TAG, "Error observing pending operations", error) }
                    .collect { Sync.requestImmediate(context = appContext) }
            }
        }
    }

    fun stop() {
        synchronized(this) {
            job?.cancel()
            job = null
            started = false
        }
    }

    private companion object {
        const val TAG = "PendingOpsScheduler"
    }
}
