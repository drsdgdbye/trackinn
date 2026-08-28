package pro.drsdgdbye.trackinn.ui.todo

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow

private fun luminance(color: Color): Float {
    fun channel(v: Float): Float {
        val c = v.coerceIn(0f, 1f)
        return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

private fun contrastRatio(a: Color, b: Color): Float {
    val la = luminance(a)
    val lb = luminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun deadlineColor(
    dueDate: Long?,
    dueTime: Long?,
    safeColor: Color,
    warningColor: Color,
    dangerColor: Color
): Color {
    if (dueDate == null) return safeColor
    val deadline = if (dueTime != null) {
        val dateCal = Calendar.getInstance().apply { timeInMillis = dueDate }
        val timeCal = Calendar.getInstance().apply { timeInMillis = dueTime }
        Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
        }.timeInMillis
    } else {
        val dateCal = Calendar.getInstance().apply { timeInMillis = dueDate }
        Calendar.getInstance().apply {
            set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
            set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
    }
    val now = System.currentTimeMillis()
    val diff = deadline - now
    val hoursRemaining = diff / (1000 * 60 * 60)
    return when {
        hoursRemaining > 24 -> safeColor
        hoursRemaining > 12 -> {
            val t = (24 - hoursRemaining.toFloat()) / 12
            lerp(safeColor, warningColor, t)
        }
        hoursRemaining > 0 -> {
            val t = (12 - hoursRemaining.toFloat()) / 12
            lerp(warningColor, dangerColor, t)
        }
        else -> dangerColor
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val t01 = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * t01,
        green = a.green + (b.green - a.green) * t01,
        blue = a.blue + (b.blue - a.blue) * t01
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onAddClick: () -> Unit = {},
    onTaskClick: (Long) -> Unit = {},
    viewModel: TodoViewModel = viewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val completedColorHex by viewModel.completedTaskColor.collectAsState(initial = "#9E9E9E")
    val safeColorHex by viewModel.deadlineSafeColor.collectAsState(initial = null)
    val warningColorHex by viewModel.deadlineWarningColor.collectAsState(initial = "#FFC107")
    val dangerColorHex by viewModel.deadlineDangerColor.collectAsState(initial = "#F44336")

    // Контрастные к фону цвета темы — используются по умолчанию
    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    val completedColor = remember(completedColorHex) {
        try { Color(android.graphics.Color.parseColor(completedColorHex)) } catch (e: Exception) { Color.Gray }
    }
    val safeColor = remember(safeColorHex, defaultTextColor, surfaceColor) {
        val chosen = safeColorHex?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { defaultTextColor }
        } ?: defaultTextColor
        // Если выбранный цвет плохо читается на фоне текущей темы
        // (например, чёрный на тёмной теме), используем контрастный цвет темы
        if (contrastRatio(chosen, surfaceColor) < 3f) defaultTextColor else chosen
    }
    val warningColor = remember(warningColorHex) {
        try { Color(android.graphics.Color.parseColor(warningColorHex)) } catch (e: Exception) { Color(0xFFFFC107) }
    }
    val dangerColor = remember(dangerColorHex) {
        try { Color(android.graphics.Color.parseColor(dangerColorHex)) } catch (e: Exception) { Color(0xFFF44336) }
    }

    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val mutableTasks = tasks.toMutableList()
        val item = mutableTasks.removeAt(from.index)
        mutableTasks.add(to.index, item)
        viewModel.updatePositions(mutableTasks)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.todo_add))
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(tasks, key = { it.id }) { task ->
                ReorderableItem(reorderableState, key = task.id) { isDragging ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleDone(task) },
                        onClick = { onTaskClick(task.id) },
                        completedColor = completedColor,
                        safeColor = safeColor,
                        warningColor = warningColor,
                        dangerColor = dangerColor,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    completedColor: Color,
    safeColor: Color,
    warningColor: Color,
    dangerColor: Color,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val color = if (task.isDone) completedColor else deadlineColor(task.dueDate, task.dueTime, safeColor, warningColor, dangerColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = color,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
            )
        }
        if (task.dueDate != null) {
            Text(
                text = dateFormat.format(Date(task.dueDate)),
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (task.dueTime != null) {
            Text(
                text = timeFormat.format(Date(task.dueTime)),
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Icon(
            Icons.Default.DragHandle,
            contentDescription = stringResource(R.string.todo_add),
            modifier = dragHandleModifier.padding(start = 8.dp)
        )
    }
}
