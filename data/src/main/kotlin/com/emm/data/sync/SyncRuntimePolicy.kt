package com.emm.data.sync

interface SyncRuntimePolicy {
    val remoteEnabled: Boolean
    val modeLabel: String
}
