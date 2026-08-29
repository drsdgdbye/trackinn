package pro.drsdgdbye.trackinn.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import pro.drsdgdbye.trackinn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TodoViewModel
) {
    val context = LocalContext.current
    // Системные диалоги (DatePickerDialog и т.п.) и startActivity требуют настоящий
    // Activity-контекст: локализованный LocalContext не является Activity
    val activityContext = (LocalActivityResultRegistryOwner.current as? Context) ?: context
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var dueTime by remember { mutableStateOf<Long?>(null) }
    var addToCalendar by remember { mutableStateOf(false) }
    var isLoaded by remember { mutableStateOf(false) }
    val isNew = taskId == -1L

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LaunchedEffect(taskId) {
        if (!isNew) {
            viewModel.tasks.collect { tasks ->
                val task = tasks.find { it.id == taskId }
                if (task != null && !isLoaded) {
                    title = task.title
                    dueDate = task.dueDate
                    dueTime = task.dueTime
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
                title = { Text(if (isNew) stringResource(R.string.task_new) else stringResource(R.string.task_edit)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("task.detail.back")
                    ) {
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task.detail.title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.task_date), modifier = Modifier.weight(1f))
                if (dueDate != null) {
                    Text(
                        text = dateFormat.format(Date(dueDate!!)),
                        modifier = Modifier
                            .testTag("task.detail.due.date")
                            .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = dueDate!! }
                            DatePickerDialog(
                                activityContext,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                    dueDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                    IconButton(
                        onClick = { dueDate = null },
                        modifier = Modifier.testTag("task.detail.due.date.clear")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                    }
                } else {
                    IconButton(
                        onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            activityContext,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                dueDate = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                        modifier = Modifier.testTag("task.detail.due.date.pick")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_date))
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.task_time), modifier = Modifier.weight(1f))
                if (dueTime != null) {
                    Text(
                        text = timeFormat.format(Date(dueTime!!)),
                        modifier = Modifier
                            .testTag("task.detail.due.time")
                            .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = dueTime!! }
                            TimePickerDialog(
                                activityContext,
                                { _, h, m ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, h)
                                        set(Calendar.MINUTE, m)
                                    }
                                    dueTime = newCal.timeInMillis
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    )
                    IconButton(
                        onClick = { dueTime = null },
                        modifier = Modifier.testTag("task.detail.due.time.clear")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                    }
                } else {
                    IconButton(
                        onClick = {
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            activityContext,
                            { _, h, m ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                }
                                dueTime = newCal.timeInMillis
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                        modifier = Modifier.testTag("task.detail.due.time.pick")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_time))
                    }
                }
            }

            if (dueDate != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task.detail.save")
            ) {
                    Text(stringResource(R.string.task_add_to_calendar), modifier = Modifier.weight(1f))
                    Switch(
                        checked = addToCalendar,
                        onCheckedChange = { addToCalendar = it },
                        modifier = Modifier.testTag("task.detail.calendar")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!isNew) {
                Button(
                    onClick = {
                        viewModel.tasks.value.find { it.id == taskId }?.let {
                            viewModel.deleteTask(it)
                        }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task.detail.delete")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(stringResource(R.string.task_delete))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isNew) {
                            viewModel.createTask(title.trim(), dueDate, dueTime) { onBack() }
                            if (addToCalendar && dueDate != null) {
                                val startMillis = if (dueTime != null) {
                                    val dateCal = Calendar.getInstance().apply { timeInMillis = dueDate!! }
                                    val timeCal = Calendar.getInstance().apply { timeInMillis = dueTime!! }
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                                        set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                                        set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                                        set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                } else {
                                    val dateCal = Calendar.getInstance().apply { timeInMillis = dueDate!! }
                                    Calendar.getInstance().apply {
                                        set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                                        set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                }
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    data = Uri.parse("content://com.android.calendar/events")
                                    putExtra("beginTime", startMillis)
                                    putExtra("endTime", startMillis + 30 * 60 * 1000L)
                                    putExtra("title", title.trim())
                                    putExtra("allDay", dueTime == null)
                                }
                                activityContext.startActivity(intent)
                            }
                        } else {
                            viewModel.tasks.value.find { it.id == taskId }?.let { existing ->
                                viewModel.updateTask(
                                    existing.copy(
                                        title = title.trim(),
                                        dueDate = dueDate,
                                        dueTime = dueTime
                                    )
                                )
                                if (addToCalendar && dueDate != null) {
                                    val startMillis = if (dueTime != null) {
                                        val dateCal = Calendar.getInstance().apply { timeInMillis = dueDate!! }
                                        val timeCal = Calendar.getInstance().apply { timeInMillis = dueTime!! }
                                        Calendar.getInstance().apply {
                                            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                                            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                                            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                                            set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                                            set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    } else dueDate!!
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        data = Uri.parse("content://com.android.calendar/events")
                                        putExtra("beginTime", startMillis)
                                        putExtra("endTime", startMillis + 30 * 60 * 1000L)
                                        putExtra("title", title.trim())
                                        putExtra("allDay", false)
                                    }
                                    activityContext.startActivity(intent)
                                }
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
