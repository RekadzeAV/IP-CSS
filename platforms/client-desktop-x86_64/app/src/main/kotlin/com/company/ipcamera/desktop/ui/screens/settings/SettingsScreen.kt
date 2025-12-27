package com.company.ipcamera.desktop.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.viewmodel.SettingsUiState
import com.company.ipcamera.desktop.ui.viewmodel.SettingsViewModel
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.loadSettings() },
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
                ErrorView(
                    message = uiState.message,
                    onRetry = { viewModel.loadSettings() }
                )
            }
            is SettingsUiState.Success -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Боковая панель с категориями
                    SettingsCategoriesList(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            selectedCategory = category
                            viewModel.setSelectedCategory(category)
                        },
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                    )

                    Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

                    // Контент настроек
                    SettingsContent(
                        settings = uiState.settings,
                        systemSettings = uiState.systemSettings,
                        selectedCategory = selectedCategory,
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoriesList(
    selectedCategory: SettingsCategory?,
    onCategorySelected: (SettingsCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CategoryButton(
                category = null,
                label = "Все",
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )

            SettingsCategory.values().forEach { category ->
                CategoryButton(
                    category = category,
                    label = category.name,
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: SettingsCategory?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
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
private fun SettingsContent(
    settings: List<Settings>,
    systemSettings: SystemSettings?,
    selectedCategory: SettingsCategory?,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Системные настройки
        systemSettings?.let { sysSettings ->
            SystemSettingsCard(
                systemSettings = sysSettings,
                onUpdate = { updated ->
                    viewModel.updateSystemSettings(
                        settings = updated,
                        onError = {}
                    )
                }
            )
        }

        // Настройки по категориям
        val filteredSettings = if (selectedCategory != null) {
            settings.filter { it.category == selectedCategory }
        } else {
            settings
        }

        val settingsByCategory = filteredSettings.groupBy { it.category }

        settingsByCategory.forEach { (category, categorySettings) ->
            SettingsCategoryCard(
                category = category,
                settings = categorySettings,
                onSettingUpdate = { key, value ->
                    viewModel.updateSetting(
                        key = key,
                        value = value,
                        onError = {}
                    )
                }
            )
        }

        if (filteredSettings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Настройки не найдены",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemSettingsCard(
    systemSettings: SystemSettings,
    onUpdate: (SystemSettings) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Системные настройки",
                style = MaterialTheme.typography.titleLarge
            )
            Divider()

            // Настройки записи
            systemSettings.recording?.let { recording ->
                Text(
                    text = "Запись",
                    style = MaterialTheme.typography.titleMedium
                )
                // Здесь можно добавить редактирование настроек записи
                Text(
                    text = "Качество по умолчанию: ${recording.defaultQuality}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Формат по умолчанию: ${recording.defaultFormat}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Настройки хранилища
            systemSettings.storage?.let { storage ->
                Text(
                    text = "Хранилище",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Использовано: ${formatBytes(storage.currentStorageUsed)} / ${formatBytes(storage.maxStorageSize)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Путь: ${storage.storagePath}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    category: SettingsCategory,
    settings: List<Settings>,
    onSettingUpdate: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge
            )
            Divider()

            settings.forEach { setting ->
                SettingRow(
                    setting = setting,
                    onValueChange = { newValue ->
                        onSettingUpdate(setting.key, newValue)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    setting: Settings,
    onValueChange: (String) -> Unit
) {
    var value by remember(setting.value) { mutableStateOf(setting.value) }
    var isEditing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = setting.key,
                style = MaterialTheme.typography.bodyLarge
            )
            setting.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isEditing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.width(200.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        onValueChange(value)
                        isEditing = false
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Сохранить")
                }
                IconButton(
                    onClick = {
                        value = setting.value
                        isEditing = false
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена")
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(200.dp)
                )
                IconButton(
                    onClick = { isEditing = true }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$bytes bytes"
    }
}
