package com.ghmanager.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ghmanager.app.data.local.TokenEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Securely stores GitHub Personal Access Tokens using EncryptedSharedPreferences.
 * Tokens are encrypted at rest; only non-sensitive metadata (name, username) is persisted.
 */
class TokenStore(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "gh_secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val metaPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("gh_token_meta", Context.MODE_PRIVATE)
    }

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<TokenEntity>>() {}.type

    private fun readMeta(): MutableList<TokenEntity> {
        val json = metaPrefs.getString(KEY_META, null)
        if (json.isNullOrBlank()) return mutableListOf()
        return try {
            gson.fromJson<MutableList<TokenEntity>>(json, listType) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun writeMeta(tokens: List<TokenEntity>) {
        metaPrefs.edit().putString(KEY_META, gson.toJson(tokens)).apply()
    }

    suspend fun saveToken(entity: TokenEntity, plainToken: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(keyFor(entity.id), plainToken).apply()
        val tokens = readMeta()
        tokens.removeAll { it.id == entity.id }
        tokens.add(entity)
        writeMeta(tokens)
    }

    suspend fun getPlainToken(tokenId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(keyFor(tokenId), null)
    }

    suspend fun deleteToken(tokenId: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(keyFor(tokenId)).apply()
        val tokens = readMeta()
        tokens.removeAll { it.id == tokenId }
        writeMeta(tokens)
    }

    suspend fun getAllTokens(): List<TokenEntity> = withContext(Dispatchers.IO) {
        readMeta()
    }

    private fun keyFor(id: String) = "tok_$id"

    companion object {
        private const val KEY_META = "token_meta"
    }
}
