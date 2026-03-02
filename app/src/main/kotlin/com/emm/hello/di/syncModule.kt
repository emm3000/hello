package com.emm.hello.di

import com.emm.data.deck.DeckSynchronizer
import com.emm.data.deck.RemoteDataSource
import com.emm.data.deck.RequestDataProcessor
import com.emm.data.flashcard.ExampleSynchronizer
import com.emm.data.flashcard.FlashcardReviewSynchronizer
import com.emm.data.flashcard.FlashcardSynchronizer
import com.emm.data.quote.QuoteSynchronizer
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val syncModule = module {
    factoryOf(::DeckSynchronizer)
    factoryOf(::FlashcardReviewSynchronizer)
    factoryOf(::FlashcardSynchronizer)
    factoryOf(::ExampleSynchronizer)
    factoryOf(::QuoteSynchronizer)
    factoryOf(::RemoteDataSource)

    factoryOf(::RequestDataProcessor)
}
