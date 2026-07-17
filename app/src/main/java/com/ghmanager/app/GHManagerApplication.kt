package com.ghmanager.app

import android.app.Application
import com.ghmanager.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GHManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GHManagerApplication)
            modules(appModule)
        }
    }
}
