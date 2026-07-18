package com.ghmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ghmanager.app.ui.MainViewModel
import com.ghmanager.app.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/** Palette for the settings sheet, so it can adapt to light/dark theme. */
private class SheetPalette(
    val panel: Color,
    val field: Color,
    val accent: Color,
    val text1: Color,
    val text2: Color,
    val line: Color
)

private val DarkSheet = SheetPalette(
    panel = Color(0xFF131A26),
    field = Color(0xFF0E1420),
    accent = Color(0xFF3B82F6),
    text1 = Color(0xFFE8ECF2),
    text2 = Color(0xFF8A94A6),
    line = Color(0xFF232D3D)
)

private val LightSheet = SheetPalette(
    panel = Color(0xFFFFFFFF),
    field = Color(0xFFF0F3F6),
    accent = Color(0xFF2563EB),
    text1 = Color(0xFF1F2328),
    text2 = Color(0xFF57606A),
    line = Color(0xFFD0D7DE)
)

@Composable
private fun rememberSheetPalette(): SheetPalette {
    // AppTheme sets a dark background (#0D1117) or light (#F6F8FA); use that to pick.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) DarkSheet else LightSheet
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val c = rememberSheetPalette()

    var name by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Bottom-aligned panel that looks like a bottom sheet but uses the
        // proven Dialog window path (avoids ModalBottomSheet + AppCompat theme crash).
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(c.panel)
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { /* consume clicks so taps inside don't dismiss */ }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(top = 12.dp, bottom = 28.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(c.line)
                    )
                }

                Text(
                    "Tokens",
                    color = c.text1,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 22.dp)
                )

                // Theme
                SectionLabel("THEME", c)
                ThemeSegmentedControl(
                    selected = themeMode,
                    onSelect = { viewModel.setThemeMode(it) },
                    c = c
                )
                Spacer(Modifier.height(22.dp))

                // Saved tokens
                SectionLabel("SAVED", c)
                if (tokens.isEmpty()) {
                    Text(
                        "No tokens saved.",
                        color = c.text2,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp)
                    )
                } else {
                    tokens.forEach { tok ->
                        TokenRow(
                            name = tok.name,
                            onDelete = { viewModel.removeToken(tok.id) },
                            c = c
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))

                // New token
                SectionLabel("NEW TOKEN", c)
                SheetField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Name",
                    c = c
                )
                Spacer(Modifier.height(8.dp))
                SheetField(
                    value = token,
                    onValueChange = { token = it },
                    placeholder = "GitHub PAT",
                    isPassword = true,
                    c = c
                )
                Spacer(Modifier.height(14.dp))

                // Save location
                LocationRow(
                    path = defaultSaveUri?.let { android.net.Uri.parse(it).lastPathSegment ?: it }
                        ?: "not set",
                    onChange = onChangeSaveLocation,
                    c = c
                )
                Spacer(Modifier.height(6.dp))

                // Footer
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Close",
                        color = c.text2,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                    PrimaryButton(
                        label = "Add",
                        enabled = name.isNotBlank() && token.isNotBlank() && !isBusy,
                        onClick = {
                            val n = name.trim()
                            val t = token.trim()
                            scope.launch {
                                val ok = viewModel.addToken(n, t)
                                if (ok) { name = ""; token = "" }
                            }
                        },
                        c = c
                    )
                }

                if (isBusy) {
                    Text(
                        "Validating token...",
                        color = c.text2,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, c: SheetPalette) {
    Text(
        text,
        color = c.text2,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun ThemeSegmentedControl(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    c: SheetPalette
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.field)
            .padding(3.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isActive = mode == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isActive) c.accent else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (isActive) Color.White else c.text2,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TokenRow(
    name: String,
    onDelete: () -> Unit,
    c: SheetPalette
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.field)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = c.text1,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Delete token",
            tint = c.text2,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDelete() }
                .padding(6.dp)
                .size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    c: SheetPalette
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = c.text2, fontSize = 14.sp) },
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = c.text1,
            unfocusedTextColor = c.text1,
            focusedContainerColor = c.field,
            unfocusedContainerColor = c.field,
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.line,
            cursorColor = c.accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LocationRow(
    path: String,
    onChange: () -> Unit,
    c: SheetPalette
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.field)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Save location - $path",
            color = c.text2,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Change",
            color = c.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onChange() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    c: SheetPalette
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) c.accent else c.accent.copy(alpha = 0.45f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 22.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
