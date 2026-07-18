package com.ghmanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.ui.MainViewModel
import com.ghmanager.app.ui.components.RepoActionsSheet

private val TagPublic = Color(0xFF2E7D32)
private val TagPrivate = Color(0xFFC62828)
private val TagPages = Color(0xFFE65100)

@Composable
fun ExistingReposTab(viewModel: MainViewModel) {
    val repos by viewModel.existingRepos.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<GithubRepo?>(null) }
    var legendOpen by remember { mutableStateOf(false) }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { legendOpen = true }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Tag legend")
                }
                IconButton(
                    onClick = { viewModel.refreshRepos() },
                    enabled = !isBusy,
                    modifier = Modifier.semantics { testTag = "repos_refresh"; contentDescription = "repos_refresh" }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
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

    if (legendOpen) {
        TagLegendDialog(onDismiss = { legendOpen = false })
    }
}

@Composable
private fun RepoCard(repo: GithubRepo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .semantics { testTag = "repo_card_${repo.name}"; contentDescription = "repo_card_${repo.name}" }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = repo.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            RepoTags(repo)
        }
    }
}

@Composable
private fun RepoTags(repo: GithubRepo) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (repo.isPrivate) Tag("PRIV", TagPrivate) else Tag("PUB", TagPublic)
        if (repo.hasPages) Tag("PAGES", TagPages)
    }
}

@Composable
private fun Tag(label: String, color: Color) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun TagLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tag legend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendRow("PUB", TagPublic, "Public repository — visible to everyone.")
                LegendRow("PRIV", TagPrivate, "Private repository — only you / collaborators.")
                LegendRow("PAGES", TagPages, "GitHub Pages is published for this repo.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun LegendRow(label: String, color: Color, meaning: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Tag(label, color)
        Spacer(modifier = Modifier.width(10.dp))
        Text(meaning, style = MaterialTheme.typography.bodyMedium)
    }
}
