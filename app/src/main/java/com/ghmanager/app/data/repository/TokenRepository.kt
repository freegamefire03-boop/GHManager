package com.ghmanager.app.data.repository

import com.ghmanager.app.data.local.TokenEntity
import com.ghmanager.app.security.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages the list of stored tokens and the currently active token.
 * The active token drives all GitHub API calls across the app.
 */
class TokenRepository(
    private val tokenStore: TokenStore
) {
    private val _tokens = MutableStateFlow<List<TokenEntity>>(emptyList())
    val tokens: StateFlow<List<TokenEntity>> = _tokens.asStateFlow()

    private val _activeTokenId = MutableStateFlow<String?>(null)
    val activeTokenId: StateFlow<String?> = _activeTokenId.asStateFlow()

    suspend fun refresh() {
        _tokens.value = tokenStore.getAllTokens()
        if (_activeTokenId.value == null && _tokens.value.isNotEmpty()) {
            _activeTokenId.value = _tokens.value.first().id
        }
    }

    suspend fun addToken(name: String, plainToken: String, username: String): TokenEntity {
        val entity = TokenEntity(id = UUID.randomUUID().toString(), name = name, username = username)
        tokenStore.saveToken(entity, plainToken)
        refresh()
        if (_activeTokenId.value == null) _activeTokenId.value = entity.id
        return entity
    }

    suspend fun removeToken(entity: TokenEntity) {
        tokenStore.deleteToken(entity.id)
        if (_activeTokenId.value == entity.id) {
            _activeTokenId.value = _tokens.value.firstOrNull { it.id != entity.id }?.id
        }
        refresh()
    }

    suspend fun setActiveToken(tokenId: String) {
        _activeTokenId.value = tokenId
    }

    suspend fun getPlainToken(tokenId: String): String? {
        return tokenStore.getPlainToken(tokenId)
    }

    fun getActiveTokenEntity(): TokenEntity? {
        val id = _activeTokenId.value ?: return null
        return _tokens.value.firstOrNull { it.id == id }
    }
}
