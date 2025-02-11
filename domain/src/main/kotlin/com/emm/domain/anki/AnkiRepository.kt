package com.emm.domain.anki

interface AnkiRepository {

    suspend fun createCard(input: String): Anki
}