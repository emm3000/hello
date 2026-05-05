package com.emm.hello.di

import android.content.Context
import android.content.SharedPreferences
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.data.deck.DefaultDeckSelectionPreferencesRepository
import com.emm.data.flashcard.DefaultFlashcardDuplicateRepository
import com.emm.data.flashcard.DefaultFlashcardRepository
import com.emm.data.flashcard.DefaultFlashcardReviewRepository
import com.emm.data.localfirst.DefaultLocalIdentityInitializer
import com.emm.data.localfirst.LocalDeviceIdentityProvider
import com.emm.data.localfirst.LocalIdentityInitializer
import com.emm.data.remote.DataStore
import com.emm.data.remote.provideSharedPreferences
import com.emm.domain.deck.CreateDeckUseCase
import com.emm.domain.deck.DeckRepository
import com.emm.domain.deck.DefaultDeckSelectionRepository
import com.emm.domain.deck.GetDeckDetailUseCase
import com.emm.domain.deck.GetDecksUseCase
import com.emm.domain.deck.GetDefaultDeckUseCase
import com.emm.domain.deck.SetDefaultDeckUseCase
import com.emm.domain.flashcard.CreateFlashcardUseCase
import com.emm.domain.flashcard.FlashcardDuplicateRepository
import com.emm.domain.flashcard.FlashcardGenerationRepository
import com.emm.domain.flashcard.FlashcardReadRepository
import com.emm.domain.flashcard.FlashcardReviewRepository
import com.emm.domain.flashcard.FlashcardWriteRepository
import com.emm.domain.flashcard.GenerateLearningNotePreviewUseCase
import com.emm.domain.flashcard.GetFlashcardByIdUseCase
import com.emm.domain.flashcard.GetStudySessionUseCase
import com.emm.domain.flashcard.IsExactDuplicateGeneratedNoteUseCase
import com.emm.domain.flashcard.ObserveFlashcardsWithReviewUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteClozeUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteExampleUseCase
import com.emm.domain.flashcard.RegenerateLearningNoteFieldUseCase
import com.emm.domain.flashcard.RegenerateStudyCardUseCase
import com.emm.domain.flashcard.StudySessionRepository
import com.emm.domain.flashcard.UpdateFlashcardReviewUseCase
import com.emm.domain.flashcard.ValidateFlashcardGenerationInputUseCase
import com.emm.domain.flashcard.ValidateGeneratedLearningNoteUseCase
import com.emm.domain.study.ScheduleFlashcardReviewUseCase
import com.emm.domain.time.Clock
import com.emm.domain.time.SystemClock
import com.emm.hello.BuildConfig
import com.emm.hello.newfeatures.card.FlashcardDetailViewModel
import com.emm.hello.newfeatures.card.NewCardGenerationDependencies
import com.emm.hello.newfeatures.card.NewCardViewModel
import com.emm.hello.newfeatures.dashboard.DashboardViewModel
import com.emm.hello.newfeatures.deck.DeckDetailViewModel
import com.emm.hello.newfeatures.deck.NewDeckViewModel
import com.emm.hello.newfeatures.study.StudyViewModel
import com.emm.hello.startup.AppStartupCoordinator
import com.emm.hello.startup.AppStartupViewModel
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
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
    single<SharedPreferences> { provideSharedPreferences(androidContext()) }

    factoryOf(::DefaultDeckRepository) bind DeckRepository::class
    factoryOf(::DefaultDeckSelectionPreferencesRepository) bind DefaultDeckSelectionRepository::class
    factoryOf(::DefaultFlashcardRepository)
    factory<FlashcardReadRepository> { get<DefaultFlashcardRepository>() }
    factory<FlashcardWriteRepository> { get<DefaultFlashcardRepository>() }
    factory<StudySessionRepository> { get<DefaultFlashcardRepository>() }
    factory<FlashcardGenerationRepository> { get<DefaultFlashcardRepository>() }
    factoryOf(::DefaultFlashcardDuplicateRepository) bind FlashcardDuplicateRepository::class
    factoryOf(::DefaultFlashcardReviewRepository) bind FlashcardReviewRepository::class
    factoryOf(::DefaultLocalIdentityInitializer) bind LocalIdentityInitializer::class
    factoryOf(::LocalDeviceIdentityProvider)
    factoryOf(::DataStore)
    single { AppStartupCoordinator(get()) }
}

fun Module.useCases() {
    single<Clock> { SystemClock }
    factoryOf(::CreateDeckUseCase)
    factoryOf(::GetDecksUseCase)
    factoryOf(::GetDefaultDeckUseCase)
    factoryOf(::SetDefaultDeckUseCase)
    factoryOf(::CreateFlashcardUseCase)
    factoryOf(::IsExactDuplicateGeneratedNoteUseCase)
    factoryOf(::ValidateFlashcardGenerationInputUseCase)
    factoryOf(::ValidateGeneratedLearningNoteUseCase)
    factoryOf(::GenerateLearningNotePreviewUseCase)
    factoryOf(::RegenerateLearningNoteExampleUseCase)
    factoryOf(::RegenerateLearningNoteClozeUseCase)
    factoryOf(::RegenerateLearningNoteFieldUseCase)
    factoryOf(::RegenerateStudyCardUseCase)
    factory {
        NewCardGenerationDependencies(
            createFlashcardUseCase = get(),
            generateLearningNotePreviewUseCase = get(),
            regenerateLearningNoteExampleUseCase = get(),
            regenerateLearningNoteClozeUseCase = get(),
            regenerateLearningNoteFieldUseCase = get(),
            regenerateStudyCardUseCase = get(),
            validateInputUseCase = get(),
            validateGeneratedLearningNoteUseCase = get(),
        )
    }
    factoryOf(::GetStudySessionUseCase)
    factoryOf(::GetDeckDetailUseCase)
    factoryOf(::GetFlashcardByIdUseCase)
    factoryOf(::UpdateFlashcardReviewUseCase)
    factoryOf(::ObserveFlashcardsWithReviewUseCase)
    factoryOf(::ScheduleFlashcardReviewUseCase)
}

fun Module.viewModels() {
    viewModel { AppStartupViewModel(get()) }
    viewModel { NewDeckViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel {
        NewCardViewModel(
            getDecksUseCase = get(),
            generationDependencies = get(),
            getDefaultDeckUseCase = get(),
            setDefaultDeckUseCase = get(),
        )
    }
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
