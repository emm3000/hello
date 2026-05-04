package com.emm.hello.sync

import com.emm.data.sync.SyncRuntimePolicy

class AppBuildSyncRuntimePolicy(
    localOnlyMode: Boolean,
) : SyncRuntimePolicy {
    override val remoteEnabled: Boolean = !localOnlyMode
    override val modeLabel: String = if (localOnlyMode) "local-only" else "remote"
}
