package com.ghmanager.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.MainViewModel
import com.ghmanager.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun TokenSettingsDialog(
    viewModel: MainViewModel,
    defaultSaveUri: String?,
    onChangeSaveLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens by viewModel.tokens.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tokens") },
        text = {
            Column {
                Text("Theme (follows phone default unless changed):", modifier = Modifier.padding(bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        TextButton(
                            onClick = { viewModel.setThemeMode(mode) },
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                Text("Stored tokens (encrypted):", modifier = Modifier.padding(bottom = 4.dp))
                tokens.forEach { tok ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("• ${tok.name} (${tok.username})", modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.removeToken(tok.id) }) { Text("Remove") }
                    }
                }
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Add a new token:", modifier = Modifier.padding(bottom = 4.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Token name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = token, onValueChange = { token = it },
                    label = { Text("GitHub PAT") }, modifier = Modifier.fillMaxWidth())
                if (isBusy) Text("Validating token…", modifier = Modifier.padding(top = 4.dp))

                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Clone save location:", modifier = Modifier.padding(bottom = 4.dp))
                Text(
                    defaultSaveUri?.let { android.net.Uri.parse(it).lastPathSegment ?: it }
                        ?: "Not set — you'll be asked on first clone",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Button(onClick = onChangeSaveLocation, modifier = Modifier.fillMaxWidth()) {
                    Text("Change default save location")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && token.isNotBlank() && !isBusy,
                onClick = {
                    val n = name.trim()
                    val t = token.trim()
                    scope.launch {
                        val ok = viewModel.addToken(n, t)
                        if (ok) {
                            name = ""
                            token = ""
                        }
                    }
                }
            ) { Text("Add Token") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
