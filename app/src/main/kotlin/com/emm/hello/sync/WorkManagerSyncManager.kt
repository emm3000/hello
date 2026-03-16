package com.emm.hello.sync

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

class WorkManagerSyncManager(context: Context) {

    val isSyncing: Flow<Boolean> = WorkManager
        .getInstance(context)
        .getWorkInfosForUniqueWorkFlow(SYNC_WORK_NAME)
        .map(List<WorkInfo>::anyRunning)
        .conflate()
}

private fun List<WorkInfo>.anyRunning() = any { it.state == WorkInfo.State.RUNNING }
