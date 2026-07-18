package com.ghmanager.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tokens")
data class TokenEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String
)

@Entity(tableName = "repo_history")
data class RepoHistoryEntity(
    @PrimaryKey val fullName: String,
    val name: String,
    val owner: String,
    val description: String?,
    val isPrivate: Boolean,
    val cloneUrl: String,
    val defaultBranch: String = "main",
    val hasPages: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val tokenId: String
)

@Entity(tableName = "action_log")
data class ActionLogEntity(
    val repoFullName: String,
    val action: String,
    val tokenId: String,
    val success: Boolean,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val uid: Int = 0
)
