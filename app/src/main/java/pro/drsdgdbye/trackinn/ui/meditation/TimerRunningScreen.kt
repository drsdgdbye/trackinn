package pro.drsdgdbye.trackinn.ui.meditation

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.drsdgdbye.trackinn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerRunningScreen(
    timerId: Long,
    onBack: () -> Unit,
    viewModel: MeditationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(timerId) {
        if (uiState.state == TimerState.IDLE) {
            viewModel.startTimerById(timerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.timer?.name ?: stringResource(R.string.meditation)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopTimer()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState.state) {
                TimerState.PREP -> {
                    Text(
                        text = "${uiState.prepRemaining}",
                        fontSize = 64.sp
                    )
                    Text(stringResource(R.string.timer_prep))
                }
                TimerState.RUNNING, TimerState.PAUSED -> {
                    val checkpoints = uiState.timer?.checkpointMinutes
                        ?.split(",")
                        ?.mapNotNull { it.trim().toIntOrNull() }
                        ?.sorted() ?: emptyList()

                    val progressColor = uiState.timer?.timerProgressColor?.let {
                        try { Color(it.toColorInt()) } catch (e: Exception) { Color(0xFF4CAF50) }
                    } ?: Color(0xFF4CAF50)

                    // Текст таймера рисуется на фоне экрана, поэтому цвет должен
                    // контрастировать с фоном темы, а не с цветом кольца прогресса
                    val timerTextColor = MaterialTheme.colorScheme.onSurface

                    ProgressRing(
                        progress = if (uiState.totalSeconds > 0) {
                            (uiState.totalSeconds - uiState.remainingSeconds).toFloat() / uiState.totalSeconds
                        } else 0f,
                        progressColor = progressColor,
                        checkpoints = checkpoints,
                        totalSeconds = uiState.totalSeconds,
                        currentCheckpointIndex = uiState.currentCheckpointIndex,
                        checkpointPassedColor = uiState.timer?.checkpointPassedColor?.let {
                            try { Color(it.toColorInt()) } catch (e: Exception) { Color(0xFF4CAF50) }
                        } ?: Color(0xFF4CAF50),
                        checkpointPendingColor = uiState.timer?.checkpointPendingColor?.let {
                            try { Color(it.toColorInt()) } catch (e: Exception) { Color(0xFF9E9E9E) }
                        } ?: Color(0xFF9E9E9E),
                        modifier = Modifier.size(250.dp)
                    ) {
                        val minutes = uiState.remainingSeconds / 60
                        val seconds = uiState.remainingSeconds % 60
                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            fontSize = 48.sp,
                            color = timerTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (uiState.state == TimerState.RUNNING) {
                            Button(onClick = { viewModel.pauseTimer() }) {
                                Text(stringResource(R.string.timer_pause))
                            }
                        } else {
                            Button(onClick = { viewModel.resumeTimer() }) {
                                Text(stringResource(R.string.timer_resume))
                            }
                        }
                        Button(onClick = {
                            viewModel.stopTimer()
                            onBack()
                        }) {
                            Text(stringResource(R.string.timer_stop))
                        }
                    }
                }
                TimerState.COMPLETED -> {
                    Text(stringResource(R.string.timer_done), fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.resetCompleted()
                        onBack()
                    }) {
                        Text(stringResource(R.string.close))
                    }
                }
                TimerState.IDLE -> {
                    Text(stringResource(R.string.timer_loading))
                }
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    checkpoints: List<Int> = emptyList(),
    totalSeconds: Int = 0,
    currentCheckpointIndex: Int = -1,
    checkpointPassedColor: Color = Color(0xFF4CAF50),
    checkpointPendingColor: Color = Color(0xFF9E9E9E),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val center = Offset(size.width / 2, size.height / 2)
            val radius = diameter / 2

            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )

            if (checkpoints.isNotEmpty() && totalSeconds > 0) {
                checkpoints.forEachIndexed { index, minute ->
                    val checkpointSeconds = minute * 60
                    val angle = -90f + (checkpointSeconds.toFloat() / totalSeconds) * 360f
                    val rad = Math.toRadians(angle.toDouble())
                    val markerX = center.x + (radius * kotlin.math.cos(rad)).toFloat()
                    val markerY = center.y + (radius * kotlin.math.sin(rad)).toFloat()
                    val color = if (index <= currentCheckpointIndex) checkpointPassedColor else checkpointPendingColor
                    drawCircle(
                        color = color,
                        radius = strokeWidth * 1.2f,
                        center = Offset(markerX, markerY)
                    )
                }
            }
        }
        content()
    }
}
