package com.emm.hello.features.backup.domain

import android.net.Uri

interface LocalStorageRepository {

    suspend fun save()

    suspend fun read(uri: Uri)

    suspend fun readPower()
}