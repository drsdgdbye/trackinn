package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.MealRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository
import pro.drsdgdbye.trackinn.ui.stats.StatsPeriod
import java.time.LocalDate
import java.time.ZoneId

data class DashboardStats(
    val averageCalories: Int = 0,
    val currentStreak: Int = 0,
    val daysLogged: Int = 0,
    val daysTotal: Int = 0,
    val validDaysPercent: Int = 0
)

data class WeeklyStat(
    val weekStart: LocalDate,
    val totalCalories: Int,
    val totalProtein: Int = 0,
    val totalFat: Int = 0,
    val totalCarbs: Int = 0
)

data class DailyStat(
    val date: LocalDate,
    val calories: Int,
    val protein: Int = 0,
    val fat: Int = 0,
    val carbs: Int = 0
)

data class DaySummary(
    val date: LocalDate,
    val calories: Int,
    val goal: Int,
    val goalMet: Boolean
)

private data class DashboardTriple(
    val dashboard: DashboardStats,
    val weekly: List<WeeklyStat>,
    val daily: List<DailyStat>
)

class CalorieHistoryViewModel(
    private val mealRepository: MealRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.MONTH)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

    private val _dateFilter = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateFilter: StateFlow<Pair<Long?, Long?>> = _dateFilter.asStateFlow()

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    private val _weeklyStats = MutableStateFlow<List<WeeklyStat>>(emptyList())
    val weeklyStats: StateFlow<List<WeeklyStat>> = _weeklyStats.asStateFlow()

    private val _dailyStats = MutableStateFlow<List<DailyStat>>(emptyList())
    val dailyStats: StateFlow<List<DailyStat>> = _dailyStats.asStateFlow()

    private val _filteredDays = MutableStateFlow<List<DaySummary>>(emptyList())
    val filteredDays: StateFlow<List<DaySummary>> = _filteredDays.asStateFlow()

    val caloriesDailyGoal: StateFlow<Int> = settingsRepository.caloriesDailyGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    init {
        viewModelScope.launch {
            combine(
                mealRepository.getAllItemsWithDate(),
                settingsRepository.caloriesDailyGoal,
                _selectedPeriod,
                _dateFilter
            ) { items, goal, period, (filterStart, filterEnd) ->
                val now = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val filteredItems = if (filterStart != null && filterEnd != null) {
                    CalorieStats.filterByEpochRange(items, filterStart, filterEnd, zone)
                } else {
                    items
                }
                val dashboard = CalorieStats.computeDashboardStats(items, now, period, zone, goal)
                val weekly = CalorieStats.computeWeeklyStats(items, now, period, zone)
                val daily = CalorieStats.computeDailyStats(items, now, period, zone)
                val days = CalorieStats.dailyCaloriesMap(filteredItems, zone)
                    .map { (date, calories) ->
                        DaySummary(
                            date = date,
                            calories = calories,
                            goal = goal,
                            goalMet = CalorieStats.isValidDay(calories, goal)
                        )
                    }
                    .sortedByDescending { it.date }
                DashboardTriple(dashboard, weekly, daily) to days
            }.collect { (triple, days) ->
                _dashboardStats.value = triple.dashboard
                _weeklyStats.value = triple.weekly
                _dailyStats.value = triple.daily
                _filteredDays.value = days
            }
        }
    }

    fun setPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
    }

    fun setDateFilter(start: Long?, end: Long?) {
        _dateFilter.value = start to end
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                CalorieHistoryViewModel(container.mealRepository, container.settingsRepository)
            }
        }
    }
}
