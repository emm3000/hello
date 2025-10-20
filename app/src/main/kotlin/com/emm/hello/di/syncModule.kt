package com.emm.hello.di

import com.emm.data.deck.DeckSynchronizer
import com.emm.data.deck.RemoteDataSource
import com.emm.data.flashcard.ExampleSynchronizer
import com.emm.data.flashcard.FlashcardReviewSynchronizer
import com.emm.data.flashcard.FlashcardSynchronizer
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val syncModule = module {
    factoryOf(::DeckSynchronizer)
    factoryOf(::FlashcardReviewSynchronizer)
    factoryOf(::FlashcardSynchronizer)
    factoryOf(::ExampleSynchronizer)
    factoryOf(::RemoteDataSource)
}