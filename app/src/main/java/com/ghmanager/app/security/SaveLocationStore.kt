package com.ghmanager.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the user's chosen default "Clone to Phone" save directory (as a
 * content URI string from the Storage Access Framework). On first run this is
 * empty, prompting the user to pick a folder.
 */
class SaveLocationStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gh_save_location",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun getDefaultUri(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_URI, null)
    }

    suspend fun setDefaultUri(uri: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_URI, uri).apply()
    }

    suspend fun hasDefault(): Boolean = getDefaultUri() != null

    companion object {
        private const val KEY_URI = "default_save_uri"
    }
}
