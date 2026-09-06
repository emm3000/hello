package com.emm.hello

import android.app.Application
import com.emm.domain.reminder.SyncStudyReminderUseCase
import com.emm.hello.di.newModule
import com.emm.hello.di.repositoryModule
import com.emm.hello.notifications.ensureStudyReminderChannel
import com.emm.hello.startup.AppStartupCoordinator
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheck()
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        FirebaseAnalytics.getInstance(this)
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                repositoryModule,
                newModule,
            )
        }
        ensureStudyReminderChannel(this)
        GlobalContext.get().get<AppStartupCoordinator>().start()
        GlobalContext.get().get<SyncStudyReminderUseCase>().invoke()
    }

    private fun installAppCheck() {
        val factory: AppCheckProviderFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}
