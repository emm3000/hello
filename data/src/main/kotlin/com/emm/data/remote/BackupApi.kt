package com.emm.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BackupApi {

    @POST("/hello")
    suspend fun createBackup(@Body request: SyncRequest): SyncStatusResponse

    @GET("/hello")
    suspend fun fetchSync(@Query("androidId") androidId: String): FetchSyncResponse
}