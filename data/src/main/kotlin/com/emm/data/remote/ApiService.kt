package com.emm.data.remote

import com.emm.data.deck.CreateDeckRequest
import com.emm.data.flashcard.CreateExampleRequest
import com.emm.data.flashcard.CreateFlashcardRequest
import com.emm.data.flashcard.CreateFlashcardReviewRequest
import com.emm.data.quote.CreateQuoteRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @POST("/hello")
    suspend fun createBackup(@Body request: SyncRequest): SyncStatusResponse

    @GET("/hello")
    suspend fun fetchSync(@Query("androidId") androidId: String): FetchSyncResponse

    @POST("decks/all")
    suspend fun createDecks(@Body body: List<CreateDeckRequest>): ResponseBody

    @POST("flashcards/all")
    suspend fun createFlashcard(@Body body: List<CreateFlashcardRequest>): ResponseBody

    @POST("examples/all")
    suspend fun createExamples(@Body body: List<CreateExampleRequest>): ResponseBody

    @POST("reviews/all")
    suspend fun createReviews(@Body body: List<CreateFlashcardReviewRequest>): ResponseBody

    @POST("quotes/all")
    suspend fun createQuotes(@Body body: List<CreateQuoteRequest>): ResponseBody

    @GET("exports")
    @Streaming
    suspend fun export(): ResponseBody
}
