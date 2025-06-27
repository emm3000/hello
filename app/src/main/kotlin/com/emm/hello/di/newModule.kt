package com.emm.hello.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.HelloDb
import com.emm.data.deck.DefaultDeckRepository
import com.emm.domain.deck.DeckCreator
import com.emm.domain.deck.DeckFetcher
import com.emm.domain.deck.DeckRepository
import com.emm.hello.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
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

    factoryOf(::DefaultDeckRepository) bind DeckRepository::class
}

fun Module.useCases() {

    factoryOf(::DeckCreator)
    factoryOf(::DeckFetcher)
}

fun Module.viewModels() {

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