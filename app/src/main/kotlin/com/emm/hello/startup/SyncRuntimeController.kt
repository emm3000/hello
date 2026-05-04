package com.emm.hello.startup

import com.emm.hello.sync.PendingOperationsSyncScheduler
import com.emm.hello.sync.SyncWorkScheduler

interface SyncRuntimeController {
    fun start()
    fun stop()
}

class DefaultSyncRuntimeController(
    private val syncWorkScheduler: SyncWorkScheduler,
    private val pendingOperationsSyncScheduler: PendingOperationsSyncScheduler,
) : SyncRuntimeController {

    override fun start() {
        syncWorkScheduler.initialize()
        pendingOperationsSyncScheduler.start()
    }

    override fun stop() {
        pendingOperationsSyncScheduler.stop()
        syncWorkScheduler.shutdown()
    }
}
