package com.ghmanager.app.di

import android.content.Context
import androidx.room.Room
import com.ghmanager.app.data.local.AppDatabase
import com.ghmanager.app.data.repository.GithubRepository
import com.ghmanager.app.data.repository.HistoryRepository
import com.ghmanager.app.data.repository.TokenRepository
import com.ghmanager.app.security.TokenStore
import com.ghmanager.app.security.SaveLocationStore
import com.ghmanager.app.security.ThemeStore
import com.ghmanager.app.ui.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { androidContext().getSharedPreferences("gh_prefs", Context.MODE_PRIVATE) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "gh_manager_db"
        ).fallbackToDestructiveMigration().build()
    }

    single { TokenStore(androidContext()) }
    single { SaveLocationStore(androidContext()) }
    single { ThemeStore(androidContext()) }
    single { GithubRepository() }
    single { HistoryRepository(get()) }
    single { TokenRepository(get()) }

    viewModel { MainViewModel(get(), get(), get(), get(), get()) }
}
