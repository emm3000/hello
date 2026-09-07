package com.emm.data.remote

interface SessionInitializer {

    suspend fun ensureSession()
}
