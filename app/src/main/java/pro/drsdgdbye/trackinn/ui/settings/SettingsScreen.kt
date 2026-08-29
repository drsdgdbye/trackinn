package pro.drsdgdbye.trackinn.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.settings.ThemeMode
import pro.drsdgdbye.trackinn.data.export.ExportImportManager

private val colorPalette = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722),
    Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFF000000)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val settings = settingsViewModel.settingsRepository
    val scope = rememberCoroutineScope()
    val exportImportManager = remember { ExportImportManager(context) }

    val pendingDisableModule by settingsViewModel.pendingDisableModule.collectAsState()
    val showDisableConfirmDialog by settingsViewModel.showDisableConfirmDialog.collectAsState()
    val wipeFailed by settingsViewModel.wipeFailed.collectAsState()

    val strWipeFailed = stringResource(R.string.module_disable_error)
    LaunchedEffect(wipeFailed) {
        if (wipeFailed) {
            Toast.makeText(context, strWipeFailed, Toast.LENGTH_LONG).show()
            settingsViewModel.consumeWipeFailed()
        }
    }

    var showImportDialog by remember { mutableStateOf(false) }

    val strImportDone = stringResource(R.string.import_done)
    val strImportError = stringResource(R.string.import_error)
    val strImportReadError = stringResource(R.string.import_read_error)
    val strImportProductsDone = stringResource(R.string.import_products_done)
    val strExportCopied = stringResource(R.string.export_copied)
    val strExportError = stringResource(R.string.export_error)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (json != null) {
                    scope.launch {
                        val result = exportImportManager.importFromJson(json)
                        if (result.isSuccess) {
                            Toast.makeText(context, strImportDone, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, String.format(strImportError, result.exceptionOrNull()?.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, strImportReadError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (json != null) {
                    scope.launch {
                        val result = exportImportManager.importProductsFromJson(json)
                        if (result.isSuccess) {
                            Toast.makeText(context, String.format(strImportProductsDone, result.getOrNull()), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, String.format(strImportError, result.exceptionOrNull()?.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, strImportReadError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val moduleTodo by settings.moduleTodo.collectAsState(initial = true)
    val moduleCalories by settings.moduleCalories.collectAsState(initial = true)
    val moduleMeditation by settings.moduleMeditation.collectAsState(initial = true)
    val moduleWeight by settings.moduleWeight.collectAsState(initial = true)
    val theme by settings.theme.collectAsState(initial = ThemeMode.SYSTEM)
    val language by settings.language.collectAsState(initial = null)
    val caloriesDailyGoal by settings.caloriesDailyGoal.collectAsState(initial = 2000)
    val weightTarget by settings.weightTarget.collectAsState(initial = 0f)
    val weightWeighInDay by settings.weightWeighInDay.collectAsState(initial = java.util.Calendar.SUNDAY)

    val completedTaskColor by settings.completedTaskColor.collectAsState(initial = "#9E9E9E")
    val deadlineSafeColor by settings.deadlineSafeColor.collectAsState(initial = null)
    val deadlineWarningColor by settings.deadlineWarningColor.collectAsState(initial = "#FFC107")
    val deadlineDangerColor by settings.deadlineDangerColor.collectAsState(initial = "#F44336")
    val progressBarColor by settings.progressBarColor.collectAsState(initial = "#4CAF50")
    val approachingGoalColor by settings.approachingGoalColor.collectAsState(initial = "#FF9800")
    val exceedingGoalColor by settings.exceedingGoalColor.collectAsState(initial = "#F44336")

    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<androidx.datastore.preferences.core.Preferences.Key<String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_modules))
            ModuleToggle(stringResource(R.string.module_todo), moduleTodo) { enabled ->
                if (enabled) settingsViewModel.enable("todo") else settingsViewModel.requestDisable("todo")
            }
            ModuleToggle(stringResource(R.string.module_calories), moduleCalories) { enabled ->
                if (enabled) settingsViewModel.enable("calories") else settingsViewModel.requestDisable("calories")
            }
            ModuleToggle(stringResource(R.string.module_meditation), moduleMeditation) { enabled ->
                if (enabled) settingsViewModel.enable("meditation") else settingsViewModel.requestDisable("meditation")
            }
            ModuleToggle(stringResource(R.string.module_weight), moduleWeight) { enabled ->
                if (enabled) settingsViewModel.enable("weight") else settingsViewModel.requestDisable("weight")
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_general))

            ThemeDropdown(theme) { scope.launch { settings.setTheme(it) } }
            LanguageDropdown(language) { lang ->
                scope.launch { settings.setLanguage(lang) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_todo))
            ColorSetting(stringResource(R.string.color_completed), completedTaskColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.COMPLETED_TASK_COLOR
                showColorPicker = true
            }
            ColorSetting(stringResource(R.string.color_no_deadline), deadlineSafeColor, defaultColor = MaterialTheme.colorScheme.onSurface) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.DEADLINE_SAFE_COLOR
                showColorPicker = true
            }
            ColorSetting(stringResource(R.string.color_deadline_start), deadlineWarningColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.DEADLINE_WARNING_COLOR
                showColorPicker = true
            }
            ColorSetting(stringResource(R.string.color_deadline_critical), deadlineDangerColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.DEADLINE_DANGER_COLOR
                showColorPicker = true
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_calories))
            NumberSetting(stringResource(R.string.daily_goal), caloriesDailyGoal) {
                scope.launch { settings.setCaloriesDailyGoal(it) }
            }
            ColorSetting(stringResource(R.string.color_progress), progressBarColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.PROGRESS_BAR_COLOR
                showColorPicker = true
            }
            ColorSetting(stringResource(R.string.color_approaching), approachingGoalColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.APPROACHING_GOAL_COLOR
                showColorPicker = true
            }
            ColorSetting(stringResource(R.string.color_exceeding), exceedingGoalColor) {
                colorPickerTarget = pro.drsdgdbye.trackinn.data.settings.SettingsKeys.EXCEEDING_GOAL_COLOR
                showColorPicker = true
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_weight))
            DecimalNumberSetting(stringResource(R.string.weight_target), weightTarget) {
                scope.launch { settings.setWeightTarget(it) }
            }
            DayDropdown(
                selectedDay = weightWeighInDay,
                onDaySelected = { scope.launch { settings.setWeightWeighInDay(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_data))
            Text(
                stringResource(R.string.export_json),
                modifier = Modifier.clickable {
                    scope.launch {
                        try {
                            val json = exportImportManager.exportToJson()
                            val fileName = "trackinn_export_${System.currentTimeMillis()}.json"
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                                putExtra(Intent.EXTRA_TITLE, fileName)
                            }
                            // For simplicity, copy to clipboard
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("trackinn_export", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, strExportCopied, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, String.format(strExportError, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.import_json),
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.import_products),
                modifier = Modifier.clickable {
                    importProductsLauncher.launch(arrayOf("application/json"))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.settings_about))
            Text(stringResource(R.string.settings_version))
        }
    }

    if (showColorPicker && colorPickerTarget != null) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                scope.launch { settings.setColor(colorPickerTarget!!, color) }
                showColorPicker = false
            }
        )
    }

    if (showDisableConfirmDialog && pendingDisableModule != null) {
        val messageRes = when (pendingDisableModule) {
            "todo" -> R.string.module_disable_todo_message
            "calories" -> R.string.module_disable_calories_message
            "meditation" -> R.string.module_disable_meditation_message
            "weight" -> R.string.module_disable_weight_message
            else -> null
        }
        if (messageRes != null) {
            AlertDialog(
                onDismissRequest = settingsViewModel::cancelDisable,
                title = { Text(stringResource(R.string.module_disable_title)) },
                text = { Text(stringResource(messageRes)) },
                confirmButton = {
                    TextButton(onClick = settingsViewModel::confirmDisable) {
                        Text(stringResource(R.string.module_disable_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = settingsViewModel::cancelDisable) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun ModuleToggle(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = when (current) {
                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.theme_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.theme_system)) }, onClick = { onSelect(ThemeMode.SYSTEM); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.theme_light)) }, onClick = { onSelect(ThemeMode.LIGHT); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.theme_dark)) }, onClick = { onSelect(ThemeMode.DARK); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(current: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = when (current) {
                "ru" -> stringResource(R.string.language_ru)
                "en" -> stringResource(R.string.language_en)
                else -> stringResource(R.string.language_system)
            },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.language_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.language_system)) }, onClick = { onSelect(null); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.language_ru)) }, onClick = { onSelect("ru"); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.language_en)) }, onClick = { onSelect("en"); expanded = false })
        }
    }
}

@Composable
private fun ColorSetting(label: String, hexColor: String?, defaultColor: Color = Color.Gray, onClick: () -> Unit) {
    val color = hexColor?.let {
        try { Color(it.toColorInt()) } catch (e: Exception) { defaultColor }
    } ?: defaultColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
private fun NumberSetting(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        text = value.toString()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                it.toIntOrNull()?.let(onChange)
            },
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
private fun DecimalNumberSetting(label: String, value: Float, onChange: (Float) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        text = value.toString()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                it.replace(",", ".").toFloatOrNull()?.let(onChange)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(100.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDropdown(selectedDay: Int, onDaySelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val days = listOf(
        java.util.Calendar.MONDAY to R.string.day_monday,
        java.util.Calendar.TUESDAY to R.string.day_tuesday,
        java.util.Calendar.WEDNESDAY to R.string.day_wednesday,
        java.util.Calendar.THURSDAY to R.string.day_thursday,
        java.util.Calendar.FRIDAY to R.string.day_friday,
        java.util.Calendar.SATURDAY to R.string.day_saturday,
        java.util.Calendar.SUNDAY to R.string.day_sunday
    )
    val selectedLabel = days.firstOrNull { it.first == selectedDay }?.second ?: R.string.day_sunday
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selectedLabel),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.weight_weigh_in_day)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            days.forEach { (calendarDay, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onDaySelected(calendarDay)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ColorPickerDialog(onDismiss: () -> Unit, onColorSelected: (String) -> Unit) {
    var selectedColor by remember { mutableStateOf(Color.Black) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column {
                Text(stringResource(R.string.pick_color_confirm) + ": ${String.format("#%06X", 0xFFFFFF and selectedColor.hashCode())}")
                Spacer(modifier = Modifier.height(8.dp))
                for (row in colorPalette.chunked(8)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        row.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hex = String.format("#%06X", 0xFFFFFF and selectedColor.hashCode())
                onColorSelected(hex)
            }) {
                Text(stringResource(R.string.pick_color_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
