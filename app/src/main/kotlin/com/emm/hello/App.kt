package com.emm.hello

import android.app.Application
import com.emm.hello.di.newModule
import com.emm.hello.di.repositoryModule
import com.emm.hello.startup.AppStartupCoordinator
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
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
        GlobalContext.get().get<AppStartupCoordinator>().start()
    }
}
