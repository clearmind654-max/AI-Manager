package com.aimanager.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aimanager.core.model.KeyStatus
import com.aimanager.core.model.ProviderType
import com.aimanager.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme section
            item { SectionHeader("🎨 Appearance") }
            item {
                SettingsCard {
                    // Theme mode
                    SettingsDropdown(
                        label = "Theme",
                        value = uiState.themeMode.name,
                        options = ThemeMode.entries.map { it.name },
                        onSelect = { viewModel.setThemeMode(ThemeMode.valueOf(it)) }
                    )
                }
            }

            // AI Models section
            item { SectionHeader("🤖 AI Models") }
            item {
                SettingsCard {
                    SettingsSwitch(
                        label = "Background Processing",
                        description = "Keep working when screen is off",
                        checked = uiState.backgroundProcessing,
                        onCheckedChange = { viewModel.setBackgroundProcessing(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsSlider(
                        label = "Max Parallel Workers",
                        value = uiState.maxParallelWorkers.toFloat(),
                        range = 1f..5f,
                        steps = 3,
                        onValueChange = { viewModel.setMaxParallelWorkers(it.toInt()) },
                        valueText = "${uiState.maxParallelWorkers}"
                    )
                }
            }

            // API Keys section
            item { SectionHeader("🔑 API Keys") }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderType.entries.take(6).forEach { provider ->
                            AssistChip(
                                onClick = { viewModel.showAddKeyDialog(provider) },
                                label = { Text(provider.name.take(6), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // API Key list
            items(uiState.apiKeys, key = { it.id }) { key ->
                ApiKeyCard(
                    provider = key.provider.name,
                    status = key.status,
                    usedToday = key.usedToday,
                    dailyLimit = key.dailyLimit,
                    onDelete = { viewModel.deleteApiKey(key.id) }
                )
            }

            // Budget section
            item { SectionHeader("💰 Budget") }
            item {
                SettingsCard {
                    SettingsSlider(
                        label = "Daily Budget",
                        value = uiState.dailyBudget.toFloat(),
                        range = 1f..50f,
                        steps = 48,
                        onValueChange = { viewModel.setDailyBudget(it.toDouble()) },
                        valueText = "$${"%.2f".format(uiState.dailyBudget)}"
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsSlider(
                        label = "Weekly Budget",
                        value = uiState.weeklyBudget.toFloat(),
                        range = 5f..200f,
                        steps = 38,
                        onValueChange = { viewModel.setWeeklyBudget(it.toDouble()) },
                        valueText = "$${"%.2f".format(uiState.weeklyBudget)}"
                    )
                }
            }

            // Security section
            item { SectionHeader("🔒 Security") }
            item {
                SettingsCard {
                    SettingsSwitch(
                        label = "Biometric Lock",
                        description = "Require fingerprint to open app",
                        checked = uiState.biometricLock,
                        onCheckedChange = { viewModel.setBiometricLock(it) }
                    )
                }
            }

            // Notifications
            item { SectionHeader("🔔 Notifications") }
            item {
                SettingsCard {
                    SettingsSwitch(
                        label = "Notifications",
                        description = "Get notified about task completions",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Add Key Dialog
    if (uiState.showAddKeyDialog) {
        AddKeyDialog(
            provider = uiState.selectedProvider,
            keyValue = uiState.keyInputValue,
            onKeyChange = { viewModel.updateKeyInput(it) },
            isValidating = uiState.isValidating,
            validationResult = uiState.keyValidationResult,
            onAdd = { viewModel.addApiKey() },
            onDismiss = { viewModel.hideAddKeyDialog() },
            autoDetectedProvider = viewModel.autoDetectProvider(uiState.keyInputValue)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsSwitch(
    label: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    valueText: String = ""
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyCard(
    provider: String,
    status: KeyStatus,
    usedToday: Int,
    dailyLimit: Int,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (status) {
                KeyStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                KeyStatus.RATE_LIMITED -> MaterialTheme.colorScheme.error
                KeyStatus.EXPIRED -> MaterialTheme.colorScheme.error
                KeyStatus.INVALID -> MaterialTheme.colorScheme.error
                KeyStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val statusIcon = when (status) {
                KeyStatus.ACTIVE -> "🟢"
                KeyStatus.RATE_LIMITED -> "🟡"
                KeyStatus.EXPIRED -> "🔴"
                KeyStatus.INVALID -> "🔴"
                KeyStatus.CHECKING -> "⏳"
            }

            Text(statusIcon, modifier = Modifier.padding(end = 8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "$usedToday / $dailyLimit calls today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { (usedToday.toFloat() / dailyLimit.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(4.dp),
                    color = statusColor
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddKeyDialog(
    provider: ProviderType?,
    keyValue: String,
    onKeyChange: (String) -> Unit,
    isValidating: Boolean,
    validationResult: String?,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    autoDetectedProvider: ProviderType?
) {
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add API Key") },
        text = {
            Column {
                provider?.let {
                    Text("Provider: ${it.name}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = keyValue,
                    onValueChange = onKeyChange,
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                )

                autoDetectedProvider?.let {
                    if (provider == null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Auto-detected: ${it.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                validationResult?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                if (isValidating) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Validating key...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd, enabled = keyValue.isNotBlank() && !isValidating) {
                Text("Add Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
