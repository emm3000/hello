package com.emm.hello

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.emm.hello.di.casesModule
import com.emm.hello.di.dataModule
import com.emm.hello.di.homeModule
import com.emm.hello.di.networkModule
import com.emm.hello.di.newModule
import com.emm.hello.di.repositoryModule
import com.emm.hello.sync.Sync
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                homeModule,
                dataModule,
                repositoryModule,
                casesModule,
                newModule,
                networkModule,
            )
        }
        registerActivityLifecycleCallbacks(this)
        Sync.initialize(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is MainActivity) {
            Sync.backupInitialize(this)
        }
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}