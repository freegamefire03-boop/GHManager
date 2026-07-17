package com.ghmanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.ui.MainViewModel
import com.ghmanager.app.ui.components.RepoActionsSheet

@Composable
fun ExistingReposTab(viewModel: MainViewModel) {
    val repos by viewModel.existingRepos.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<GithubRepo?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (isBusy) "Loading repositories…" else "Repositories (${repos.size})",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.refreshRepos() }, enabled = !isBusy) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        if (!isBusy && repos.isEmpty()) {
            Text("No repositories found for this token.", modifier = Modifier.padding(16.dp))
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(repos) { repo ->
                RepoCard(repo) { selected = repo }
            }
        }
    }

    selected?.let {
        RepoActionsSheet(repo = it, viewModel = viewModel, onDismiss = { selected = null })
    }
}

@Composable
private fun RepoCard(repo: GithubRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = repo.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp)
        )
    }
}
