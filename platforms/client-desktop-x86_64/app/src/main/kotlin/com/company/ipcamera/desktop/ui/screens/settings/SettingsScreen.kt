package com.company.ipcamera.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.theme.LocalThemeMode
import com.company.ipcamera.desktop.ui.theme.ThemeMode
import com.company.ipcamera.desktop.ui.viewmodel.SettingsUiState
import com.company.ipcamera.desktop.ui.viewmodel.SettingsViewModel
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (viewModel.showSaveSuccess) {
            // Показать snackbar при успешном сохранении
            viewModel.dismissSaveSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.loadSettings(viewModel.selectedCategory) },
                enabled = uiState !is SettingsUiState.Loading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Divider()

        // Контент
        when (uiState) {
            is SettingsUiState.Loading -> {
                LoadingIndicator(message = "Загрузка настроек...")
            }
            is SettingsUiState.Error -> {
                val errorState = uiState as SettingsUiState.Error
                ErrorView(
                    message = errorState.message,
                    onRetry = { viewModel.loadSettings(viewModel.selectedCategory) }
                )
            }
            is SettingsUiState.Success -> {
                val successState = uiState as SettingsUiState.Success
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Боковая панель категорий
                    Surface(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SettingsCategoryButton(
                                category = null,
                                label = "Все",
                                selected = viewModel.selectedCategory == null,
                                onClick = { viewModel.setCategory(null) }
                            )
                            SettingsCategory.values().forEach { category ->
                                SettingsCategoryButton(
                                    category = category,
                                    label = getCategoryDisplayName(category),
                                    selected = viewModel.selectedCategory == category,
                                    onClick = { viewModel.setCategory(category) }
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                    // Список настроек
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (successState.settings.isEmpty()) {
                            EmptySettingsView()
                        } else {
                            SettingsList(
                                settings = successState.settings,
                                onSettingChange = { key, value ->
                                    viewModel.updateSetting(key, value)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryButton(
    category: SettingsCategory?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(label)
    }
}

@Composable
private fun SettingsList(
    settings: List<Settings>,
    onSettingChange: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(settings) { setting ->
            SettingItem(
                setting = setting,
                onValueChange = { value ->
                    onSettingChange(setting.key, value)
                }
            )
        }
    }
}

@Composable
private fun SettingItem(
    setting: Settings,
    onValueChange: (String) -> Unit
) {
    var value by remember(setting.value) { mutableStateOf(setting.value) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = setting.key,
                style = MaterialTheme.typography.titleMedium
            )

            setting.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (setting.type) {
                com.company.ipcamera.shared.domain.model.SettingsType.BOOLEAN -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Включено")
                        Switch(
                            checked = value.toBooleanStrictOrNull() ?: false,
                            onCheckedChange = { checked ->
                                value = checked.toString()
                                onValueChange(value)
                            }
                        )
                    }
                }
                com.company.ipcamera.shared.domain.model.SettingsType.INTEGER -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                value = newValue
                                onValueChange(value)
                            }
                        },
                        label = { Text("Значение") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                com.company.ipcamera.shared.domain.model.SettingsType.FLOAT -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                value = newValue
                                onValueChange(value)
                            }
                        },
                        label = { Text("Значение") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { newValue ->
                            value = newValue
                            onValueChange(value)
                        },
                        label = { Text("Значение") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (setting.type == com.company.ipcamera.shared.domain.model.SettingsType.JSON) 5 else 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptySettingsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нет настроек",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Настройки будут отображаться здесь",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getCategoryDisplayName(category: SettingsCategory): String {
    return when (category) {
        SettingsCategory.RECORDING -> "Запись"
        SettingsCategory.STORAGE -> "Хранилище"
        SettingsCategory.NOTIFICATIONS -> "Уведомления"
        SettingsCategory.SECURITY -> "Безопасность"
        SettingsCategory.NETWORK -> "Сеть"
        SettingsCategory.SYSTEM -> "Система"
        SettingsCategory.ANALYTICS -> "Аналитика"
        SettingsCategory.OTHER -> "Прочее"
    }
}