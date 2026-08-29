package pro.drsdgdbye.trackinn.ui.meditation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerEditorScreen(
    timerId: Long,
    onBack: () -> Unit,
    viewModel: MeditationViewModel
) {
    var name by remember { mutableStateOf("") }
    var totalMinutes by remember { mutableStateOf("") }
    var prepSeconds by remember { mutableStateOf("") }
    var checkpoints by remember { mutableStateOf("") }
    var startSound by remember { mutableStateOf("meditation_start") }
    var endSound by remember { mutableStateOf("meditation_end") }
    var checkpointSound by remember { mutableStateOf("meditation_checkpoint") }
    var timerProgressColor by remember { mutableStateOf("#4CAF50") }
    var checkpointPassedColor by remember { mutableStateOf("#4CAF50") }
    var checkpointPendingColor by remember { mutableStateOf("#9E9E9E") }
    var isLoaded by remember { mutableStateOf(false) }
    val isNew = timerId == -1L

    val sounds = listOf("meditation_start", "meditation_end", "meditation_checkpoint")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val errorEnterName = stringResource(R.string.error_enter_name)
    val errorMinTime = stringResource(R.string.error_min_time)
    val errorCheckpointsIntegers = stringResource(R.string.error_checkpoints_integers)
    val errorCheckpointsRange = stringResource(R.string.error_checkpoints_range)

    LaunchedEffect(timerId) {
        if (!isNew) {
            viewModel.timers.collect { timers ->
                val timer = timers.find { it.id == timerId }
                if (timer != null && !isLoaded) {
                    name = timer.name
                    totalMinutes = timer.totalMinutes.toString()
                    prepSeconds = timer.prepSeconds.toString()
                    checkpoints = timer.checkpointMinutes
                    startSound = timer.startSound ?: "meditation_start"
                    endSound = timer.endSound ?: "meditation_end"
                    checkpointSound = timer.checkpointSound ?: "meditation_checkpoint"
                    timerProgressColor = timer.timerProgressColor
                    checkpointPassedColor = timer.checkpointPassedColor
                    checkpointPendingColor = timer.checkpointPendingColor
                    isLoaded = true
                }
            }
        } else {
            isLoaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) stringResource(R.string.timer_new) else stringResource(R.string.timer_edit)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("timer.editor.back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.timer_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer.editor.name")
            )
            OutlinedTextField(
                value = totalMinutes,
                onValueChange = { totalMinutes = it },
                label = { Text(stringResource(R.string.timer_total_minutes)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer.editor.minutes")
            )
            OutlinedTextField(
                value = prepSeconds,
                onValueChange = { prepSeconds = it },
                label = { Text(stringResource(R.string.timer_prep_seconds)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer.editor.prep")
            )
            OutlinedTextField(
                value = checkpoints,
                onValueChange = { checkpoints = it },
                label = { Text(stringResource(R.string.timer_checkpoints)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer.editor.checkpoints")
            )

            Spacer(modifier = Modifier.height(16.dp))

            SoundDropdown(stringResource(R.string.sound_start), startSound, sounds, "timer.editor.sound.start") { startSound = it }
            SoundDropdown(stringResource(R.string.sound_end), endSound, sounds, "timer.editor.sound.end") { endSound = it }
            SoundDropdown(stringResource(R.string.sound_checkpoint), checkpointSound, sounds, "timer.editor.sound.checkpoint") { checkpointSound = it }

            Spacer(modifier = Modifier.weight(1f))

            if (!isNew) {
                Button(
                    onClick = {
                        viewModel.timers.value.find { it.id == timerId }?.let {
                            viewModel.deleteTimer(it)
                        }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timer.editor.delete")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(stringResource(R.string.timer_delete_label))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val minutes = totalMinutes.toIntOrNull()
                    if (name.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(errorEnterName) }
                        return@Button
                    }
                    if (minutes == null || minutes < 5) {
                        scope.launch { snackbarHostState.showSnackbar(errorMinTime) }
                        return@Button
                    }
                    if (checkpoints.isNotBlank()) {
                        val checkpointValues = checkpoints.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (checkpointValues.size != checkpoints.split(",").size) {
                            scope.launch { snackbarHostState.showSnackbar(errorCheckpointsIntegers) }
                            return@Button
                        }
                        val invalid = checkpointValues.filter { it <= 0 || it >= minutes }
                        if (invalid.isNotEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(errorCheckpointsRange.format(invalid.joinToString(), minutes - 1)) }
                            return@Button
                        }
                    }
                    val timer = SavedTimerEntity(
                        id = if (isNew) 0 else timerId,
                        name = name.trim(),
                        totalMinutes = minutes,
                        prepSeconds = prepSeconds.toIntOrNull() ?: 0,
                        checkpointMinutes = checkpoints,
                        startSound = startSound,
                        endSound = endSound,
                        checkpointSound = checkpointSound,
                        timerProgressColor = timerProgressColor,
                        checkpointPassedColor = checkpointPassedColor,
                        checkpointPendingColor = checkpointPendingColor,
                        position = 0
                    )
                    if (isNew) {
                        viewModel.createTimer(timer)
                    } else {
                        viewModel.updateTimer(timer)
                    }
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer.editor.save")
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundDropdown(label: String, current: String, options: List<String>, testTag: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .testTag(testTag)
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
