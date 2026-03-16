package com.emm.hello.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.deck.DefaultDeckSelectionPreferencesRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.OperationLogWriter
import com.emm.data.quote.DefaultQuoteRepository
import com.emm.data.remote.DataStore
import com.emm.data.remote.DefaultBackupRepository
import com.emm.data.sync.AckOperations
import com.emm.data.sync.ApplyRemoteOperation
import com.emm.data.sync.DefaultPairingRepository
import com.emm.data.sync.DefaultSyncDebugStateRepository
import com.emm.data.sync.DefaultSyncEngine
import com.emm.data.sync.DrainOutbox
import com.emm.data.sync.PullRemoteOperations
import com.emm.data.sync.syncDataModule
import com.emm.domain.backup.BackupRepository
import com.emm.domain.backup.RunBackupUseCase
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.FlashcardRepository
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.GenerateFlashcardPreviewUseCase
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.quote.GenerateQuoteUseCase
import com.emm.domain.quote.ObserveLatestQuoteUseCase
import com.emm.domain.quote.QuoteRepository
import com.emm.domain.quote.SaveQuoteAsFlashcardUseCase
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.sync.CreatePairingSessionUseCase
import com.emm.domain.sync.EnsureLinkedIdentityUseCase
import com.emm.domain.sync.GetSyncDebugStateUseCase
import com.emm.domain.sync.ListLinkedDevicesUseCase
import com.emm.domain.sync.PairingRepository
import com.emm.domain.sync.RedeemPairingCodeUseCase
import com.emm.domain.sync.RevokeLinkedDeviceUseCase
import com.emm.domain.sync.SyncDebugStateRepository
import com.emm.domain.sync.SyncEngine
import com.emm.hello.BuildConfig
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.pairing.PairingViewModel
import com.emm.hello.newfeatures.study.StudyViewModel
import com.emm.hello.sync.PendingOperationsSyncScheduler
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val newModule = module {
    includes(syncDataModule)

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
    factoryOf(::DefaultDeckSelectionPreferencesRepository) bind DefaultDeckSelectionRepository::class
    factoryOf(::DefaultFlashcardRepository) bind FlashcardRepository::class
    factoryOf(::DefaultQuoteRepository) bind QuoteRepository::class
    factoryOf(::DefaultBackupRepository) bind BackupRepository::class
    factoryOf(::DefaultFlashcardReviewRepository) bind FlashcardReviewRepository::class
    factoryOf(::DefaultSyncEngine) bind SyncEngine::class
    factoryOf(::DefaultSyncDebugStateRepository) bind SyncDebugStateRepository::class
    factoryOf(::DefaultPairingRepository) bind PairingRepository::class

    factoryOf(::DrainOutbox)
    factoryOf(::PullRemoteOperations)
    factoryOf(::ApplyRemoteOperation)
    factoryOf(::AckOperations)

    factoryOf(::LocalDeviceIdentityProvider)
    factoryOf(::OperationLogWriter)
    factoryOf(::DataStore)
    single { PendingOperationsSyncScheduler(androidContext(), get()) }
}

fun Module.useCases() {
    factoryOf(::CreateDeckUseCase)
    factoryOf(::GetDecksUseCase)
    factoryOf(::GetDefaultDeckUseCase)
    factoryOf(::SetDefaultDeckUseCase)
    factoryOf(::CreateFlashcardUseCase)
    factoryOf(::GenerateFlashcardPreviewUseCase)
    factoryOf(::GetStudySessionUseCase)
    factoryOf(::GetDeckDetailUseCase)
    factoryOf(::GetFlashcardByIdUseCase)
    factoryOf(::GenerateQuoteUseCase)
    factoryOf(::ObserveLatestQuoteUseCase)
    factoryOf(::SaveQuoteAsFlashcardUseCase)
    factoryOf(::RunBackupUseCase)
    factoryOf(::UpdateFlashcardReviewUseCase)
    factoryOf(::ObserveFlashcardsWithReviewUseCase)
    factoryOf(::GetSyncDebugStateUseCase)
    factoryOf(::EnsureLinkedIdentityUseCase)
    factoryOf(::CreatePairingSessionUseCase)
    factoryOf(::RedeemPairingCodeUseCase)
    factoryOf(::ListLinkedDevicesUseCase)
    factoryOf(::RevokeLinkedDeviceUseCase)
    factoryOf(::ScheduleFlashcardReviewUseCase)
}

fun Module.viewModels() {
    viewModelOf(::NewDeckViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::PairingViewModel)
    viewModelOf(::NewCardViewModel)
    viewModel {
        StudyViewModel(
            deckId = it.get(),
            getStudySessionUseCase = get(),
            scheduleFlashcardReviewUseCase = get(),
            updateFlashcardReviewUseCase = get(),
        )
    }
    viewModel {
        DeckDetailViewModel(
            deckId = it.get(),
            getDeckDetailUseCase = get(),
            observeFlashcardsWithReviewUseCase = get(),
        )
    }
    viewModel {
        FlashcardDetailViewModel(
            flashcardId = it.get(),
            getFlashcardByIdUseCase = get(),
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
