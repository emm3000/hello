package com.emm.hello

import android.app.Application
import com.emm.hello.di.networkModule
import com.emm.hello.di.newModule
import com.emm.hello.di.repositoryModule
import com.emm.hello.sync.PendingOperationsSyncScheduler
import com.emm.hello.sync.Sync
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                repositoryModule,
                newModule,
                networkModule,
            )
        }
        Sync.initialize(this)
        GlobalContext.get().get<PendingOperationsSyncScheduler>().start()
    }

    override fun onTerminate() {
        super.onTerminate()
        GlobalContext.get().get<PendingOperationsSyncScheduler>().stop()
    }
}
