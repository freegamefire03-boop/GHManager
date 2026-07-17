package com.ghmanager.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.screens.CreateRepoTab
import com.ghmanager.app.ui.screens.ExistingReposTab
import com.ghmanager.app.ui.screens.HistoryTab
import com.ghmanager.app.ui.components.TokenSettingsDialog
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = koinViewModel()) {
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val activeTokenId by viewModel.activeTokenId.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val showWarning by viewModel.showSwitchWarning.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val needsSaveLocation by viewModel.needsSaveLocation.collectAsStateWithLifecycle()
    val pendingCloneWithUri by viewModel.pendingCloneWithUri.collectAsStateWithLifecycle()
    val defaultSaveUri by viewModel.defaultSaveUri.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val pagerState = rememberPagerState(pageCount = { 3 })
    val pagerScope = rememberCoroutineScope()
    var tokenMenuOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    val activeToken = tokens.firstOrNull { it.id == activeTokenId }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist access so we can write on future launches
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.onSaveLocationResolved(uri.toString())
        } else {
            viewModel.cancelSaveLocation()
        }
    }

    LaunchedEffect(Unit) { viewModel.init() }

    LaunchedEffect(message) {
        message?.let {
            kotlinx.coroutines.delay(4000)
            viewModel.clearMessage()
        }
    }

    // First-run: ask for a save folder before cloning
    LaunchedEffect(needsSaveLocation) {
        if (needsSaveLocation) treeLauncher.launch(null)
    }

    // Default save location already set: perform the clone into it
    LaunchedEffect(pendingCloneWithUri) {
        pendingCloneWithUri?.let { (uri, repo) ->
            viewModel.cloneRepoToUri(context, repo, uri)
            viewModel.consumePendingCloneWithUri()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Manager") },
                actions = {
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
            Column {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { pagerScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Create Repo") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { pagerScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("History") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { pagerScope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("REPOS") }
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> CreateRepoTab(viewModel)
                        1 -> HistoryTab(viewModel)
                        2 -> ExistingReposTab(viewModel)
                    }
                }
            }

            message?.let {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        containerColor = if (it.isError)
                            MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
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
                TokenSettingsDialog(
                    viewModel = viewModel,
                    defaultSaveUri = defaultSaveUri,
                    onChangeSaveLocation = {
                        treeLauncher.launch(null)
                    },
                    onDismiss = { settingsOpen = false }
                )
            }
        }
    }
}
