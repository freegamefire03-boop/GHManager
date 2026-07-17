package com.ghmanager.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.screens.CreateRepoTab
import com.ghmanager.app.ui.screens.ExistingReposTab
import com.ghmanager.app.ui.screens.HistoryTab
import com.ghmanager.app.ui.components.TokenSettingsDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val activeTokenId by viewModel.activeTokenId.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val showWarning by viewModel.showSwitchWarning.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var tabIndex by remember { mutableIntStateOf(0) }
    var tokenMenuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val activeToken = tokens.firstOrNull { it.id == activeTokenId }

    LaunchedEffect(Unit) { viewModel.init() }

    LaunchedEffect(message) {
        message?.let {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Manager") },
                actions = {
                    // Global token switcher
                    Box {
                        TextButton(onClick = { tokenMenuOpen = true }) {
                            Text(activeToken?.name ?: "No token")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = tokenMenuOpen,
                            onDismissRequest = { tokenMenuOpen = false }
                        ) {
                            if (tokens.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No tokens — add in Settings") },
                                    onClick = { tokenMenuOpen = false }
                                )
                            }
                            tokens.forEach { tok ->
                                DropdownMenuItem(
                                    text = { Text("${tok.name} (${tok.username})") },
                                    onClick = {
                                        tokenMenuOpen = false
                                        viewModel.requestSwitchToken(tok.id)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            androidx.compose.foundation.layout.Column {
                TabRow(selectedTabIndex = tabIndex) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Create Repo") })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("History") })
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }, text = { Text("Existing") })
                }
                when (tabIndex) {
                    0 -> CreateRepoTab(viewModel)
                    1 -> HistoryTab(viewModel)
                    2 -> ExistingReposTab(viewModel)
                }
            }

            // Snackbar for messages
            message?.let {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                    Snackbar(
                        containerColor = if (it.isError)
                            androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                        else androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(it.text)
                    }
                }
            }

            if (showWarning) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelSwitch() },
                    title = { Text("Action in progress") },
                    text = { Text("An action is currently in progress. Switching tokens now will cancel your current progress. Do you want to proceed?") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmSwitchDespiteBusy() }) { Text("Switch Anyway") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelSwitch() }) { Text("Cancel") }
                    }
                )
            }

            if (settingsOpen) {
                TokenSettingsDialog(viewModel = viewModel, onDismiss = { settingsOpen = false })
            }
        }
    }
}
