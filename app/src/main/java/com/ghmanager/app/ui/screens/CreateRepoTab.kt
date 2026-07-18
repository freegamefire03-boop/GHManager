package com.ghmanager.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepoTab(viewModel: MainViewModel, onOpenSettings: () -> Unit = {}) {
    val activeTokenId by viewModel.activeTokenId.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var autoInit by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (activeTokenId == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Welcome! Add a GitHub Personal Access Token to get started.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Add Token")
            }
            return
        }
        Text("Create a new repository", modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Repository name *") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = description, onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        RowCheckbox("Private repository", isPrivate) { isPrivate = it }
        RowCheckbox("Initialize with README (auto_init)", autoInit) { autoInit = it }

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    viewModel.createRepo(
                        name.trim(),
                        description.ifBlank { null },
                        isPrivate,
                        autoInit
                    )
                    name = ""
                    description = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Create Repository")
        }
    }
}

@Composable
private fun RowCheckbox(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label)
    }
}
