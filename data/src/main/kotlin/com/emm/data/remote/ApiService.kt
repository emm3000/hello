package com.emm.data.remote

import com.emm.data.deck.CreateDeckRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("/hello")
    suspend fun createBackup(@Body request: SyncRequest): SyncStatusResponse

    @GET("/hello")
    suspend fun fetchSync(@Query("androidId") androidId: String): FetchSyncResponse

    @POST("decks/all")
    suspend fun createDecks(@Body body: List<CreateDeckRequest>): ResponseBody
}