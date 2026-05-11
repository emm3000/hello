package com.emm.hello.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.export.BackupExporter
import com.emm.data.export.BackupImporter
import com.emm.data.flashcard.GeminiService
import com.emm.data.localfirst.LocalIdentityInitializer
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module that overrides production bindings with deterministic fakes
 * for instrumented tests.
 *
 * Load it **after** the production modules so these bindings win:
 *
 * ```
 * KoinComposeTestRule(newModule, repositoryModule, testModule)
 * ```
 */
val testModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = HelloDb.Schema,
            context = androidContext(),
            name = ":memory:",
        )
    }
    single<HelloDb> { HelloDb(get()) }
    single<LocalIdentityInitializer> { FakeLocalIdentityInitializer() }
    single<GeminiService> { FakeGeminiService() }
    single<BackupExporter> { FakeBackupExporter() }
    single<BackupImporter> { FakeBackupImporter() }
}
