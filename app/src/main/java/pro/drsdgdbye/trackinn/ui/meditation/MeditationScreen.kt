package pro.drsdgdbye.trackinn.ui.meditation

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationScreen(
    onAddClick: () -> Unit = {},
    onTimerClick: (Long) -> Unit = {},
    onStartTimer: (Long) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    viewModel: MeditationViewModel
) {
    val timers by viewModel.timers.collectAsState()
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        val mutableTimers = timers.toMutableList()
        val item = mutableTimers.removeAt(from.index)
        mutableTimers.add(to.index, item)
        viewModel.updatePositions(mutableTimers)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meditation)) },
                actions = {
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.testTag("meditation.history")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.meditation_history))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("meditation.add")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_timer))
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(timers, key = { it.id }) { timer ->
                ReorderableItem(reorderableState, key = timer.id) { isDragging ->
                    TimerItem(
                        timer = timer,
                        onClick = { onTimerClick(timer.id) },
                        onStart = { onStartTimer(timer.id) },
                        onDelete = { viewModel.deleteTimer(timer) },
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.draggableHandle()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerItem(
    timer: SavedTimerEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meditation.timer.${timer.id}.item")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(timer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.timer_minutes_format, timer.totalMinutes) +
                        if (timer.checkpointMinutes.isNotBlank()) stringResource(R.string.checkpoints_format, timer.checkpointMinutes) else "",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(
            Icons.Default.DragHandle,
            contentDescription = null,
            modifier = dragHandleModifier
                .testTag("meditation.timer.${timer.id}.drag")
        )
        IconButton(
            onClick = onStart,
            modifier = Modifier.testTag("meditation.timer.${timer.id}.start")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.timer_start))
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("meditation.timer.${timer.id}.delete")
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.timer_delete))
        }
    }
}
