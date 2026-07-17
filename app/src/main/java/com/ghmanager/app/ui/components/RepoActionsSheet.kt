package com.ghmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var renameOpen by remember { mutableStateOf(false) }
    var transferOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Actions for ${repo.fullName}", modifier = Modifier.padding(bottom = 12.dp))
            ActionButton("Clone to Phone (download zip)") { viewModel.cloneRepo(repo); onDismiss() }
            ActionButton(if (repo.isPrivate) "Make Public" else "Make Private") {
                viewModel.changeVisibility(repo, !repo.isPrivate); onDismiss()
            }
            ActionButton("Rename") { renameOpen = true }
            ActionButton("Fork") { viewModel.forkRepo(repo); onDismiss() }
            ActionButton("Transfer Ownership") { transferOpen = true }
            ActionButton("Delete Repository") { viewModel.deleteRepo(repo); onDismiss() }
        }
    }

    if (renameOpen) {
        RenameDialog(repo = repo, viewModel = viewModel, onDismiss = { renameOpen = false })
    }
    if (transferOpen) {
        TransferDialog(repo = repo, viewModel = viewModel, onDismiss = { transferOpen = false })
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label)
    }
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
