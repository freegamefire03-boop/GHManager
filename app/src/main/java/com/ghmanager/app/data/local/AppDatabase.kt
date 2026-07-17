package com.ghmanager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TokenEntity::class, RepoHistoryEntity::class, ActionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun repoHistoryDao(): RepoHistoryDao
    abstract fun actionLogDao(): ActionLogDao
}
