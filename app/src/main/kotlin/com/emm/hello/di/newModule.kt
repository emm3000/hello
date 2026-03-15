package com.emm.hello.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.quote.DefaultQuoteRepository
import com.emm.data.remote.DataStore
import com.emm.data.remote.DefaultBackupRepository
import com.emm.domain.backup.BackupExecutor
import com.emm.domain.backup.BackupRepository
import com.emm.domain.deck.DeckCreator
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DecksWithCardsProvider
import com.emm.domain.flashcard.FlashcardAndReviewFetcher
import com.emm.domain.flashcard.FlashcardCreator
import com.emm.domain.flashcard.FlashcardFetcher
import com.emm.domain.flashcard.FlashcardFinder
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FlashcardReviewUpdater
import com.emm.domain.quote.QuoteGenerator
import com.emm.domain.quote.QuoteLastFetcher
import com.emm.domain.quote.QuoteRepository
import com.emm.hello.BuildConfig
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.study.StudyViewModel
import com.emm.hello.sync.WorkManagerSyncManager
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val newModule = module {
    single { provideSqlDriver(androidContext()) }
    single<HelloDb> { provideDb(get()) }

    repository()
    useCases()
    viewModels()
}

fun Module.repository() {
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    factoryOf(::DefaultDeckRepository) bind DeckRepository::class
    factoryOf(::DefaultFlashcardRepository) bind FlashcardRepository::class
    factoryOf(::DefaultQuoteRepository) bind QuoteRepository::class
    factoryOf(::DefaultBackupRepository) bind BackupRepository::class
    factoryOf(::DefaultFlashcardReviewRepository) bind FlashcardReviewRepository::class

    factoryOf(::LocalDeviceIdentityProvider)
    factoryOf(::DataStore)
    factoryOf(::WorkManagerSyncManager)
}

fun Module.useCases() {
    factoryOf(::DeckCreator)
    factoryOf(::DeckFetcher)
    factoryOf(::FlashcardCreator)
    factoryOf(::FlashcardFetcher)
    factoryOf(::DecksWithCardsProvider)
    factoryOf(::FlashcardFinder)
    factoryOf(::QuoteGenerator)
    factoryOf(::QuoteLastFetcher)
    factoryOf(::BackupExecutor)
    factoryOf(::FlashcardReviewUpdater)
    factoryOf(::FlashcardAndReviewFetcher)
}

fun Module.viewModels() {
    viewModelOf(::NewDeckViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::NewCardViewModel)
    viewModel {
        StudyViewModel(
            deckId = it.get(),
            flashcardFetcher = get(),
            flashcardReviewUpdater = get(),
            reviewSynchronizer = get(),
        )
    }
    viewModel {
        DeckDetailViewModel(
            deckId = it.get(),
            decksWithCardsProvider = get(),
            flashcardAndReviewFetcher = get(),
        )
    }
    viewModel {
        FlashcardDetailViewModel(
            flashcardId = it.get(),
            flashcardFinder = get(),
        )
    }
}

fun provideSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = HelloDb.Schema,
        context = context,
        name = "${BuildConfig.APPLICATION_ID}.db",
        callback = csm()
    )
}

fun csm() = object : AndroidSqliteDriver.Callback(schema = HelloDb.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}

fun provideDb(sqlDriver: SqlDriver): HelloDb = HelloDb(sqlDriver)
