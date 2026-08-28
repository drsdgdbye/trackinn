package pro.drsdgdbye.trackinn.ui.meditation

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationHistoryScreen(
    onBack: () -> Unit,
    viewModel: MeditationViewModel
) {
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val filteredSessions by viewModel.filteredSessions.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    val context = LocalContext.current
    val activityContext = (LocalActivityResultRegistryOwner.current as? Context) ?: context

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val periods = listOf(StatsPeriod.WEEK, StatsPeriod.MONTH, StatsPeriod.YEAR)
    val periodLabels = listOf(
        stringResource(R.string.tab_week),
        stringResource(R.string.tab_month),
        stringResource(R.string.tab_year)
    )
    var selectedTabIndex by remember { mutableIntStateOf(1) }

    LaunchedEffect(selectedTabIndex) {
        viewModel.setPeriod(periods[selectedTabIndex])
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meditation_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tabs
            item {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    periodLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dashboard
            item {
                DashboardSummary(stats = dashboardStats)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bar chart
            item {
                if (weeklyStats.isNotEmpty()) {
                    WeeklyBarChart(weeklyStats = weeklyStats)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Heatmap calendar
            item {
                if (dailyStats.isNotEmpty()) {
                    HeatmapCalendar(dailyStats = dailyStats)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Date filter
            item {
                DateFilterRow(
                    startDate = startDate,
                    endDate = endDate,
                    dateFormat = dateFormat,
                    activityContext = activityContext,
                    onStartChanged = { startDate = it },
                    onEndChanged = { endDate = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sessions
            if (filteredSessions.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_sessions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filteredSessions) { session ->
                    SessionItem(session = session, dateFormat = dateFormat, timeFormat = timeFormat)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardSummary(stats: DashboardStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Schedule,
            label = stringResource(R.string.stat_total_sessions),
            value = stats.totalSessions.toString()
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            label = stringResource(R.string.stat_total_time),
            value = formatMinutes(stats.totalMinutes)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalFireDepartment,
            label = stringResource(R.string.stat_streak),
            value = stringResource(R.string.stat_streak_days, stats.currentStreak)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            label = stringResource(R.string.stat_completion),
            value = stringResource(R.string.stat_percent, stats.completionRate)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(weeklyStats: List<WeeklyStat>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(weeklyStats) {
        modelProducer.runTransaction {
            columnModel {
                series(weeklyStats.map { it.totalMinutes })
            }
        }
    }

    Column {
        Text(
            text = stringResource(R.string.stat_total_time),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    }
}

@Composable
private fun HeatmapCalendar(dailyStats: List<DailyStat>) {
    val today = LocalDate.now()
    val firstDay = dailyStats.firstOrNull()?.date ?: today
    val weeks = mutableListOf<List<DailyStat>>()

    var currentWeekStart = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    while (!currentWeekStart.isAfter(today)) {
        val weekEnd = currentWeekStart.plusDays(6)
        val weekDays = (0L..6L).map { offset ->
            val date = currentWeekStart.plusDays(offset)
            val stat = dailyStats.find { it.date == date }
            stat ?: DailyStat(date, 0)
        }
        weeks.add(weekDays)
        currentWeekStart = currentWeekStart.plusWeeks(1)
    }

    val heatmapColors = listOf(
        Color(0xFFebedf0),
        Color(0xFF9be9a8),
        Color(0xFF40c463),
        Color(0xFF30a14e),
        Color(0xFF216e39)
    )

    Column {
        Text(
            text = stringResource(R.string.meditation_history),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        val intensity = when {
                            day.totalMinutes <= 0 -> 0
                            day.totalMinutes <= 10 -> 1
                            day.totalMinutes <= 20 -> 2
                            day.totalMinutes <= 30 -> 3
                            else -> 4
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatmapColors[intensity])
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            heatmapColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(2.dp))
            }
            Text(
                text = "40+",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun DateFilterRow(
    startDate: Long?,
    endDate: Long?,
    dateFormat: SimpleDateFormat,
    activityContext: Context,
    onStartChanged: (Long?) -> Unit,
    onEndChanged: (Long?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.date_from), modifier = Modifier.padding(end = 8.dp))
            if (startDate != null) {
                Text(
                    text = dateFormat.format(Date(startDate)),
                    modifier = Modifier.clickable {
                        showDatePicker(activityContext, startDate) { onStartChanged(it) }
                    }
                )
                IconButton(onClick = { onStartChanged(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            } else {
                IconButton(onClick = {
                    showDatePicker(activityContext, null) { onStartChanged(it) }
                }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_date))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.date_to), modifier = Modifier.padding(end = 8.dp))
            if (endDate != null) {
                Text(
                    text = dateFormat.format(Date(endDate)),
                    modifier = Modifier.clickable {
                        showDatePicker(activityContext, endDate) { onEndChanged(it) }
                    }
                )
                IconButton(onClick = { onEndChanged(null) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            } else {
                IconButton(onClick = {
                    showDatePicker(activityContext, null) { onEndChanged(it) }
                }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.task_select_date))
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: MeditationSessionEntity,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (session.wasCompleted) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (session.wasCompleted) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(session.startedAt)),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = timeFormat.format(Date(session.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.duration_minutes, session.durationMinutes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatMinutes(totalMinutes: Int): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
}

private fun showDatePicker(context: Context, initialDate: Long?, onDateSelected: (Long) -> Unit) {
    val cal = Calendar.getInstance()
    if (initialDate != null) {
        cal.timeInMillis = initialDate
    }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            val newCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
            onDateSelected(newCal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
