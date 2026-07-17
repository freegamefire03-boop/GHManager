package com.ghmanager.app.data.repository

import com.ghmanager.app.data.local.AppDatabase
import com.ghmanager.app.data.local.RepoHistoryEntity
import com.ghmanager.app.data.local.ActionLogEntity

class HistoryRepository(private val db: AppDatabase) {

    suspend fun getHistoryForToken(tokenId: String): List<RepoHistoryEntity> {
        return db.repoHistoryDao().getForToken(tokenId)
    }

    suspend fun recordCreatedRepo(entity: RepoHistoryEntity) {
        db.repoHistoryDao().insert(entity)
    }

    suspend fun removeFromHistory(fullName: String) {
        db.repoHistoryDao().deleteByFullName(fullName)
    }

    suspend fun clearForToken(tokenId: String) {
        db.repoHistoryDao().deleteForToken(tokenId)
        db.actionLogDao().deleteForToken(tokenId)
    }

    suspend fun logAction(entry: ActionLogEntity) {
        db.actionLogDao().insert(entry)
    }

    suspend fun getLogsForToken(tokenId: String): List<ActionLogEntity> {
        return db.actionLogDao().getForToken(tokenId)
    }
}
