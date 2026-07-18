package com.ghmanager.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ghmanager.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the user's theme preference (system / dark / light). Encrypted the
 * same way as other local settings. Defaults to SYSTEM (follow phone default).
 */
class ThemeStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "gh_theme",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    suspend fun getMode(): ThemeMode = withContext(Dispatchers.IO) {
        ThemeMode.fromKey(prefs.getString(KEY_MODE, null))
    }

    suspend fun setMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_MODE, mode.key).apply()
    }

    companion object {
        private const val KEY_MODE = "theme_mode"
    }
}
