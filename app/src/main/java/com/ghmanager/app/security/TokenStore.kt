package com.ghmanager.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ghmanager.app.data.local.TokenEntity
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

    suspend fun saveToken(entity: TokenEntity, plainToken: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(keyFor(entity.id), plainToken).apply()
        val names = metaPrefs.getStringSet(KEY_NAMES, emptySet())!!.toMutableSet()
        val meta = metaPrefs.getString(KEY_META, "")!!
        val tokens = if (meta.isBlank()) mutableListOf() else meta.split("||").toMutableList()
        tokens.removeAll { it.startsWith("${entity.id}::") }
        tokens.add("${entity.id}::${entity.name}::${entity.username}")
        names.add(entity.id)
        metaPrefs.edit()
            .putStringSet(KEY_NAMES, names)
            .putString(KEY_META, tokens.joinToString("||"))
            .apply()
    }

    suspend fun getPlainToken(tokenId: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(keyFor(tokenId), null)
    }

    suspend fun deleteToken(tokenId: String) = withContext(Dispatchers.IO) {
        prefs.edit().remove(keyFor(tokenId)).apply()
        val names = metaPrefs.getStringSet(KEY_NAMES, emptySet())!!.toMutableSet()
        names.remove(tokenId)
        val meta = metaPrefs.getString(KEY_META, "")!!
        val tokens = meta.split("||").filter { it.isNotBlank() && !it.startsWith("$tokenId::") }
        metaPrefs.edit()
            .putStringSet(KEY_NAMES, names)
            .putString(KEY_META, tokens.joinToString("||"))
            .apply()
    }

    suspend fun getAllTokens(): List<TokenEntity> = withContext(Dispatchers.IO) {
        val meta = metaPrefs.getString(KEY_META, "")!!
        meta.split("||")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("::")
                if (parts.size >= 3) {
                    TokenEntity(id = parts[0], name = parts[1], username = parts[2])
                } else null
            }
    }

    private fun keyFor(id: String) = "tok_$id"

    companion object {
        private const val KEY_NAMES = "token_ids"
        private const val KEY_META = "token_meta"
    }
}
