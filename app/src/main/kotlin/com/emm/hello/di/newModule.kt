package com.emm.hello.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.domain.deck.DeckCreator
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DecksWithCardsProvider
import com.emm.domain.flashcard.FlashcardCreator
import com.emm.domain.flashcard.FlashcardFetcher
import com.emm.domain.flashcard.FlashcardFinder
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.hello.BuildConfig
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
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
}

fun Module.useCases() {

    factoryOf(::DeckCreator)
    factoryOf(::DeckFetcher)
    factoryOf(::FlashcardCreator)
    factoryOf(::FlashcardFetcher)
    factoryOf(::DecksWithCardsProvider)
    factoryOf(::FlashcardFinder)
}

fun Module.viewModels() {

    viewModelOf(::NewDeckViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::NewCardViewModel)
    viewModel {
        DeckDetailViewModel(
            deckId = it.get(),
            decksWithCardsProvider = get(),
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