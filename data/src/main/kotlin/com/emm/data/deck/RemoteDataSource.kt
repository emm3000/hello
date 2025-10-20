package com.emm.data.deck

import com.emm.data.remote.ApiService

class RemoteDataSource(private val apiService: ApiService) {

    suspend fun createDeck(decks: List<CreateDeckRequest>) {
        apiService.createDecks(decks)
    }
}
