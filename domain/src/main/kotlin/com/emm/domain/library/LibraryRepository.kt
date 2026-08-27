package com.emm.domain.library

import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryFlashcard>>
}
