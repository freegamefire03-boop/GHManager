package com.ghmanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.MainScreen
import com.ghmanager.app.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: com.ghmanager.app.ui.MainViewModel = koinViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            AppTheme(themeMode = themeMode) {
                Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}
