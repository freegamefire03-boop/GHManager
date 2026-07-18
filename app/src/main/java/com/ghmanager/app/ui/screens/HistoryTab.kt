package com.ghmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.data.local.RepoHistoryEntity
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.ui.MainViewModel
import com.ghmanager.app.ui.components.RepoActionsSheet

@Composable
fun HistoryTab(viewModel: MainViewModel) {
    val history by viewModel.historyRepos.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<GithubRepo?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Repositories created with this app (active token)",
            modifier = Modifier.padding(16.dp)
        )
        if (history.isEmpty()) {
            Text(
                "No repositories created yet. Create one from the \"Create Repo\" tab.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn {
            items(history) { item ->
                HistoryCard(item) {
                    selected = GithubRepo(
                        name = item.name,
                        description = item.description,
                        isPrivate = item.isPrivate,
                        fullName = item.fullName,
                        cloneUrl = item.cloneUrl,
                        defaultBranch = item.defaultBranch,
                        hasPages = item.hasPages,
                        owner = com.ghmanager.app.data.remote.model.Owner(login = item.owner)
                    )
                }
            }
        }
    }

    selected?.let {
        RepoActionsSheet(repo = it, viewModel = viewModel, onDismiss = { selected = null })
    }
}

@Composable
private fun HistoryCard(item: RepoHistoryEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.fullName + if (item.isPrivate) "  (private)" else "  (public)")
            item.description?.let { Text(it) }
        }
    }
}
