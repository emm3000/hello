package com.emm.hello

import android.app.Application
import com.emm.hello.di.dataModule
import com.emm.hello.di.homeModule
import com.emm.hello.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(homeModule, dataModule, repositoryModule)
        }
    }
}