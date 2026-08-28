package pro.drsdgdbye.trackinn.ui.calorie

import pro.drsdgdbye.trackinn.data.db.dao.MealItemWithDate
import pro.drsdgdbye.trackinn.ui.stats.StatsPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Чистая логика статистики калорий, независимая от ViewModel и Android.
 * Все функции принимают now/zone/goal параметрами для детерминированного тестирования.
 */
internal object CalorieStats {

    /** Валидный день: есть записи и 60% <= ккал <= 100% цели. */
    fun isValidDay(calories: Int, goal: Int): Boolean {
        if (goal <= 0 || calories <= 0) return false
        val caloriesX100 = calories.toLong() * 100
        return caloriesX100 >= goal.toLong() * 60 && caloriesX100 <= goal.toLong() * 100
    }

    /**
     * Уровень теплокарты относительно цели:
     * 0 — белый (<60% цели или нет записей), 1 — зелёный (60–80%),
     * 2 — жёлтый (80–100%), 3 — красный (>100%).
     */
    fun heatmapLevel(calories: Int, goal: Int): Int = when {
        goal <= 0 || calories <= 0 -> 0
        else -> {
            val ratio = calories.toDouble() / goal
            when {
                ratio < 0.6 -> 0
                ratio <= 0.8 -> 1
                ratio <= 1.0 -> 2
                else -> 3
            }
        }
    }

    /** Ккал по дням (только дни с записями). */
    fun dailyCaloriesMap(
        items: List<MealItemWithDate>,
        zone: ZoneId
    ): Map<LocalDate, Int> {
        return items
            .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
            .mapValues { (_, dayItems) -> dayItems.sumOf { it.item.calories } }
    }

    fun filterByRange(
        items: List<MealItemWithDate>,
        start: LocalDate,
        end: LocalDate,
        zone: ZoneId
    ): List<MealItemWithDate> {
        return items.filter { item ->
            val itemDate = Instant.ofEpochMilli(item.date).atZone(zone).toLocalDate()
            !itemDate.isBefore(start) && !itemDate.isAfter(end)
        }
    }

    fun filterByEpochRange(
        items: List<MealItemWithDate>,
        filterStart: Long,
        filterEnd: Long,
        zone: ZoneId
    ): List<MealItemWithDate> {
        val start = Instant.ofEpochMilli(filterStart).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(filterEnd).atZone(zone).toLocalDate()
        return filterByRange(items, start, end, zone)
    }

    fun getPeriodRange(now: LocalDate, period: StatsPeriod): Pair<LocalDate, LocalDate> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weekStart to weekStart.plusDays(6)
            }
            StatsPeriod.MONTH -> {
                now.withDayOfMonth(1) to now
            }
            StatsPeriod.YEAR -> {
                val daysCount = if (now.isLeapYear) 366 else 365
                now.minusDays((daysCount - 1).toLong()) to now
            }
        }
    }

    fun computeDashboardStats(
        items: List<MealItemWithDate>,
        now: LocalDate,
        period: StatsPeriod,
        zone: ZoneId,
        goal: Int
    ): DashboardStats {
        val daily = dailyCaloriesMap(items, zone)
        val averageCalories = if (daily.isNotEmpty()) daily.values.sum() / daily.size else 0
        val streak = computeStreak(daily, now, goal)
        val (start, end) = getPeriodRange(now, period)
        val daysTotal = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
        val daysLogged = daily.keys.count { !it.isBefore(start) && !it.isAfter(end) }
        var validDays = 0
        var current = start
        while (!current.isAfter(end)) {
            if (isValidDay(daily[current] ?: 0, goal)) validDays++
            current = current.plusDays(1)
        }
        val validPercent = if (daysTotal > 0) validDays * 100 / daysTotal else 0
        return DashboardStats(
            averageCalories = averageCalories,
            currentStreak = streak,
            daysLogged = daysLogged,
            daysTotal = daysTotal,
            validDaysPercent = validPercent
        )
    }

    /** Строгий стрик: подряд идущие валидные дни, заканчивающиеся сегодня. */
    fun computeStreak(
        daily: Map<LocalDate, Int>,
        today: LocalDate,
        goal: Int
    ): Int {
        var streak = 0
        var expected = today
        while (isValidDay(daily[expected] ?: 0, goal)) {
            streak++
            expected = expected.minusDays(1)
        }
        return streak
    }

    fun computeWeeklyStats(
        items: List<MealItemWithDate>,
        now: LocalDate,
        period: StatsPeriod,
        zone: ZoneId
    ): List<WeeklyStat> {
        val daily = dailyCaloriesMap(items, zone)
        return when (period) {
            StatsPeriod.WEEK -> emptyList()
            StatsPeriod.MONTH -> {
                val weeksCount = 4
                val result = mutableListOf<WeeklyStat>()
                for (i in (weeksCount - 1) downTo 0) {
                    val weekEnd = now.minusWeeks(i.toLong())
                    val weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weekEndClamped = weekEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    var calories = 0
                    var current = weekStart
                    while (!current.isAfter(weekEndClamped)) {
                        calories += daily[current] ?: 0
                        current = current.plusDays(1)
                    }
                    result.add(WeeklyStat(weekStart, calories))
                }
                result
            }
            StatsPeriod.YEAR -> {
                val result = mutableListOf<WeeklyStat>()
                for (i in 11 downTo 0) {
                    val monthStart = now.minusMonths(i.toLong()).withDayOfMonth(1)
                    val monthEnd = monthStart.plusMonths(1).minusDays(1)
                    var calories = 0
                    var current = monthStart
                    while (!current.isAfter(monthEnd)) {
                        calories += daily[current] ?: 0
                        current = current.plusDays(1)
                    }
                    result.add(WeeklyStat(monthStart, calories))
                }
                result
            }
        }
    }

    fun computeDailyStats(
        items: List<MealItemWithDate>,
        now: LocalDate,
        period: StatsPeriod,
        zone: ZoneId
    ): List<DailyStat> {
        val (start, end) = getPeriodRange(now, period)
        val daily = dailyCaloriesMap(items, zone)
        val result = mutableListOf<DailyStat>()
        var current = start
        while (!current.isAfter(end)) {
            result.add(DailyStat(current, daily[current] ?: 0))
            current = current.plusDays(1)
        }
        return result
    }
}
