package com.ghmanager.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoActionsSheet(
    repo: GithubRepo,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var renameOpen by remember { mutableStateOf(false) }
    var transferOpen by remember { mutableStateOf(false) }
    var deleteConfirmOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(repo.name, modifier = Modifier.padding(bottom = 12.dp))

            OutlinedButton(
                onClick = {
                    val url = repo.htmlUrl.ifBlank { "https://github.com/${repo.fullName}" }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text("Open in Browser") }

            if (repo.hasPages) {
                OutlinedButton(
                    onClick = {
                        val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
                        val pagesUrl = repo.homepage?.takeIf { it.isNotBlank() && it.startsWith("http") }
                            ?: "https://$owner.github.io/${repo.name}/"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pagesUrl))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("Open Published Page") }
            }

            ActionButton("Clone to Phone (download zip)") { viewModel.cloneRepo(repo); onDismiss() }
            ActionButton("Publish (GitHub Pages)") { viewModel.publishRepo(repo); onDismiss() }
            ActionButton(if (repo.isPrivate) "Make Public" else "Make Private") {
                viewModel.changeVisibility(repo, !repo.isPrivate); onDismiss()
            }
            ActionButton("Rename") { renameOpen = true }
            ActionButton("Fork") { viewModel.forkRepo(repo); onDismiss() }
            ActionButton("Transfer Ownership") { transferOpen = true }
            ActionButton("Delete Repository") { deleteConfirmOpen = true }
        }
    }

    if (renameOpen) {
        RenameDialog(repo = repo, viewModel = viewModel, onDismiss = { renameOpen = false })
    }
    if (transferOpen) {
        TransferDialog(repo = repo, viewModel = viewModel, onDismiss = { transferOpen = false })
    }
    if (deleteConfirmOpen) {
        DeleteConfirmDialog(
            repo = repo,
            onConfirm = {
                deleteConfirmOpen = false
                viewModel.deleteRepo(repo)
                onDismiss()
            },
            onDismiss = { deleteConfirmOpen = false }
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label)
    }
}

@Composable
private fun DeleteConfirmDialog(
    repo: GithubRepo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft -= 1
        }
    }

    val enabled = secondsLeft <= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete repository?") },
        text = {
            Text("Are you sure you want to delete '${repo.fullName}'? This cannot be undone.")
        },
        confirmButton = {
            TextButton(enabled = enabled, onClick = onConfirm) {
                Text(if (enabled) "Yes, delete" else "Yes, delete ($secondsLeft)")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RenameDialog(repo: GithubRepo, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(repo.name) }
    var desc by remember { mutableStateOf(repo.description ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename / Edit") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("New name") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.renameRepo(repo, name, desc.ifBlank { null }, null)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TransferDialog(repo: GithubRepo, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var owner by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Repository") },
        text = {
            OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("New owner username/org") })
        },
        confirmButton = {
            TextButton(onClick = {
                if (owner.isNotBlank()) {
                    viewModel.transferRepo(repo, owner.trim())
                    onDismiss()
                }
            }) { Text("Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
