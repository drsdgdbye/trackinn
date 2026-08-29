package pro.drsdgdbye.trackinn.ui.calorie

import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
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
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.ui.stats.StatsPeriod
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieHistoryScreen(
    onBack: () -> Unit,
    viewModel: CalorieHistoryViewModel
) {
    val dashboardStats by viewModel.dashboardStats.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val dailyStats by viewModel.dailyStats.collectAsState()
    val filteredDays by viewModel.filteredDays.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val caloriesDailyGoal by viewModel.caloriesDailyGoal.collectAsState()

    val context = LocalContext.current
    val activityContext = (LocalActivityResultRegistryOwner.current as? Context) ?: context

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dayFormat = remember { java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy") }

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
                title = { Text(stringResource(R.string.calorie_history)) },
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

            item {
                DashboardSummary(stats = dashboardStats)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                val configuration = LocalConfiguration.current
                val chartLocale = configuration.locales[0]
                if (selectedPeriod == StatsPeriod.WEEK) {
                    if (dailyStats.isNotEmpty()) {
                        CalorieBarChart(
                            labels = dailyStats.map {
                                it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, chartLocale)
                            },
                            proteinValues = dailyStats.map { it.protein },
                            fatValues = dailyStats.map { it.fat },
                            carbsValues = dailyStats.map { it.carbs }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    if (weeklyStats.isNotEmpty()) {
                        val labels = when (selectedPeriod) {
                            StatsPeriod.MONTH -> weeklyStats.mapIndexed { index, _ -> "${index + 1}" }
                            StatsPeriod.YEAR -> weeklyStats.map {
                                Month.of(it.weekStart.monthValue).getDisplayName(TextStyle.SHORT, chartLocale)
                            }
                            else -> emptyList()
                        }
                        CalorieBarChart(
                            labels = labels,
                            proteinValues = weeklyStats.map { it.totalProtein },
                            fatValues = weeklyStats.map { it.totalFat },
                            carbsValues = weeklyStats.map { it.totalCarbs }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                if (dailyStats.isNotEmpty()) {
                    CalorieHeatmap(dailyStats = dailyStats, goal = caloriesDailyGoal)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                DateFilterRow(
                    startDate = startDate,
                    endDate = endDate,
                    dateFormat = dateFormat,
                    activityContext = activityContext,
                    onStartChanged = {
                        startDate = it
                        viewModel.setDateFilter(it, endDate)
                    },
                    onEndChanged = {
                        endDate = it
                        viewModel.setDateFilter(startDate, it)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (filteredDays.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_entries),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                items(filteredDays) { day ->
                    DayItem(day = day, dateFormat = dayFormat)
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
            label = stringResource(R.string.stat_avg_calories),
            value = stats.averageCalories.toString()
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalFireDepartment,
            label = stringResource(R.string.stat_streak),
            value = stringResource(R.string.stat_streak_days, stats.currentStreak)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CalendarMonth,
            label = stringResource(R.string.stat_days_logged),
            value = stringResource(R.string.stat_days_fraction, stats.daysLogged, stats.daysTotal)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            label = stringResource(R.string.stat_goal_days),
            value = stringResource(R.string.stat_percent, stats.validDaysPercent)
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
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
private fun CalorieBarChart(
    labels: List<String>,
    proteinValues: List<Int>,
    fatValues: List<Int>,
    carbsValues: List<Int>
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(proteinValues, fatValues, carbsValues) {
        modelProducer.runTransaction {
            columnModel {
                series(proteinValues)
                series(fatValues)
                series(carbsValues)
            }
        }
    }

    val valueFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            val index = value.toInt()
            if (index in labels.indices) labels[index] else index.toString()
        }
    }

    val proteinColor = Color(0xFF2196F3)
    val fatColor = Color(0xFFF44336)
    val carbsColor = Color(0xFFFFEB3B)

    val proteinColumn = com.patrykandpatrick.vico.compose.common.component.rememberLineComponent(
        com.patrykandpatrick.vico.compose.common.Fill(proteinColor),
        10.dp
    )
    val fatColumn = com.patrykandpatrick.vico.compose.common.component.rememberLineComponent(
        com.patrykandpatrick.vico.compose.common.Fill(fatColor),
        10.dp
    )
    val carbsColumn = com.patrykandpatrick.vico.compose.common.component.rememberLineComponent(
        com.patrykandpatrick.vico.compose.common.Fill(carbsColor),
        10.dp
    )
    val columnProvider = com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer.ColumnProvider.series(
        proteinColumn, fatColumn, carbsColumn
    )

    Column {
        Text(
            text = stringResource(R.string.stat_calories_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = columnProvider,
                    mergeMode = { com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer.MergeMode.Stacked }
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = valueFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
    }
}

@Composable
private fun CalorieHeatmap(dailyStats: List<DailyStat>, goal: Int) {
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
        Color(0xFFEBEDF0),
        Color(0xFFFFC107),
        Color(0xFF4CAF50),
        Color(0xFFF44336)
    )

    Column {
        Text(
            text = stringResource(R.string.calorie_history),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { day ->
                        val level = CalorieStats.heatmapLevel(day.calories, goal)
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatmapColors[level])
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
                text = stringResource(R.string.heatmap_legend_min),
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
                text = stringResource(R.string.heatmap_legend_max),
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
private fun DayItem(day: DaySummary, dateFormat: java.time.format.DateTimeFormatter) {
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
                imageVector = if (day.goalMet) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = stringResource(if (day.goalMet) R.string.goal_met else R.string.goal_missed),
                tint = if (day.goalMet) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = day.date.format(dateFormat),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.calories_format, day.calories, day.goal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
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
