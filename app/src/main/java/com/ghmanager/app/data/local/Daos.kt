package com.ghmanager.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TokenDao {
    @Query("SELECT * FROM tokens")
    suspend fun getAll(): List<TokenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: TokenEntity)

    @Delete
    suspend fun delete(token: TokenEntity)

    @Query("SELECT * FROM tokens WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TokenEntity?
}

@Dao
interface RepoHistoryDao {
    @Query("SELECT * FROM repo_history WHERE tokenId = :tokenId ORDER BY createdAt DESC")
    suspend fun getForToken(tokenId: String): List<RepoHistoryEntity>

    @Query("SELECT * FROM repo_history ORDER BY createdAt DESC")
    suspend fun getAll(): List<RepoHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(repo: RepoHistoryEntity)

    @Query("DELETE FROM repo_history WHERE fullName = :fullName")
    suspend fun deleteByFullName(fullName: String)

    @Query("DELETE FROM repo_history WHERE tokenId = :tokenId")
    suspend fun deleteForToken(tokenId: String)
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_log WHERE tokenId = :tokenId ORDER BY timestamp DESC")
    suspend fun getForToken(tokenId: String): List<ActionLogEntity>

    @Insert
    suspend fun insert(log: ActionLogEntity)

    @Query("DELETE FROM action_log WHERE tokenId = :tokenId")
    suspend fun deleteForToken(tokenId: String)
}
