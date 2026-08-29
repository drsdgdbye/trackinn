package pro.drsdgdbye.trackinn.ui.calorie

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.drsdgdbye.trackinn.data.db.dao.MealItemWithDate
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.ui.stats.StatsPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class CalorieStatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private val today: LocalDate = LocalDate.of(2026, 8, 28) // пятница
    private val goal: Int = 2000

    private fun itemOn(date: LocalDate, calories: Int, protein: Int = 0, fat: Int = 0, carbs: Int = 0): MealItemWithDate {
        val dateMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return MealItemWithDate(
            item = MealItemEntity(
                mealId = 1,
                name = "test",
                weight = 100,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs
            ),
            date = dateMillis
        )
    }

    private fun itemsOn(vararg entries: Pair<LocalDate, Int>): List<MealItemWithDate> =
        entries.map { (date, calories) -> itemOn(date, calories) }

    private fun itemsOnWithMacros(vararg entries: Triple<LocalDate, Int, Triple<Int, Int, Int>>): List<MealItemWithDate> =
        entries.map { (date, calories, macros) -> itemOn(date, calories, macros.first, macros.second, macros.third) }

    @Test
    fun isValidDay_below60Percent_invalid() {
        assertFalse(CalorieStats.isValidDay(1199, goal))
        assertTrue(CalorieStats.isValidDay(1200, goal))
    }

    @Test
    fun isValidDay_overGoal_invalid() {
        assertTrue(CalorieStats.isValidDay(2000, goal))
        assertFalse(CalorieStats.isValidDay(2001, goal))
    }

    @Test
    fun isValidDay_noEntries_invalid() {
        assertFalse(CalorieStats.isValidDay(0, goal))
    }

    @Test
    fun isValidDay_zeroGoal_invalid() {
        assertFalse(CalorieStats.isValidDay(1000, 0))
    }

    @Test
    fun heatmapLevel_boundaries() {
        assertEquals(0, CalorieStats.heatmapLevel(1199, goal))
        assertEquals(1, CalorieStats.heatmapLevel(1200, goal))
        assertEquals(1, CalorieStats.heatmapLevel(1600, goal))
        assertEquals(2, CalorieStats.heatmapLevel(1601, goal))
        assertEquals(2, CalorieStats.heatmapLevel(2000, goal))
        assertEquals(3, CalorieStats.heatmapLevel(2001, goal))
        assertEquals(0, CalorieStats.heatmapLevel(0, goal))
    }

    @Test
    fun averageCalories_onlyDaysWithEntries() {
        val items = itemsOn(
            today to 1000,
            today.minusDays(1) to 1500,
            today.minusDays(2) to 500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(1000, stats.averageCalories)
    }

    @Test
    fun averageCalories_empty_returnsZero() {
        val stats = CalorieStats.computeDashboardStats(emptyList(), today, StatsPeriod.WEEK, zone, goal)
        assertEquals(0, stats.averageCalories)
    }

    @Test
    fun streak_todayInvalid_returnsZero() {
        val items = itemsOn(
            today to 2001, // перебор
            today.minusDays(1) to 1500,
            today.minusDays(2) to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(0, stats.currentStreak)
    }

    @Test
    fun streak_validChainEndingToday() {
        val items = itemsOn(
            today to 1500,
            today.minusDays(1) to 1500,
            today.minusDays(2) to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun streak_breakStopsAtInvalidDay() {
        val items = itemsOn(
            today to 1500,
            today.minusDays(1) to 300, // ниже 60%
            today.minusDays(2) to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun streak_overGoalDay_breaksChain() {
        val items = itemsOn(
            today to 1500,
            today.minusDays(1) to 2500, // перебор
            today.minusDays(2) to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun daysLoggedAndTotal_weekPeriod() {
        val items = itemsOn(
            today to 1500,
            today.minusDays(2) to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(7, stats.daysTotal)
        assertEquals(2, stats.daysLogged)
    }

    @Test
    fun validDaysPercent_weekPeriod() {
        // в понедельник перебор, сегодня и вчера валидно
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = itemsOn(
            monday to 2500,
            today.minusDays(1) to 1500,
            today to 1500
        )
        val stats = CalorieStats.computeDashboardStats(items, today, StatsPeriod.WEEK, zone, goal)
        assertEquals(28, stats.validDaysPercent) // 2 валидных из 7 дней
    }

    @Test
    fun validDaysPercent_empty_returnsZero() {
        val stats = CalorieStats.computeDashboardStats(emptyList(), today, StatsPeriod.WEEK, zone, goal)
        assertEquals(0, stats.validDaysPercent)
    }

    @Test
    fun weeklyStats_monthPeriod_returnsFourWeeksWithCalorieSums() {
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = itemsOn(
            currentWeekStart to 1000,
            currentWeekStart.plusDays(2) to 500,
            currentWeekStart.minusDays(2) to 700
        )
        val result = CalorieStats.computeWeeklyStats(items, today, StatsPeriod.MONTH, zone)
        assertEquals(4, result.size)
        assertEquals(1500, result.last().totalCalories)
        assertEquals(700, result[result.size - 2].totalCalories)
        assertEquals(0, result.first().totalCalories)
    }

    @Test
    fun weeklyStats_yearPeriod_returnsTwelveMonths() {
        val items = itemsOn(today to 1000)
        val result = CalorieStats.computeWeeklyStats(items, today, StatsPeriod.YEAR, zone)
        assertEquals(12, result.size)
        assertEquals(1000, result.last().totalCalories)
    }

    @Test
    fun weeklyStats_weekPeriod_returnsEmpty() {
        val result = CalorieStats.computeWeeklyStats(emptyList(), today, StatsPeriod.WEEK, zone)
        assertTrue(result.isEmpty())
    }

    @Test
    fun dailyStats_weekPeriod_returnsSevenDaysStartingMonday() {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = itemsOn(monday to 1200, monday.plusDays(6) to 800)
        val result = CalorieStats.computeDailyStats(items, today, StatsPeriod.WEEK, zone)
        assertEquals(7, result.size)
        assertEquals(monday, result.first().date)
        assertEquals(1200, result.first().calories)
        assertEquals(800, result.last().calories)
        assertEquals(0, result[1].calories)
    }

    @Test
    fun dailyStats_monthPeriod_spansFromFirstDayToNow() {
        val result = CalorieStats.computeDailyStats(emptyList(), today, StatsPeriod.MONTH, zone)
        assertEquals(28, result.size)
        assertEquals(LocalDate.of(2026, 8, 1), result.first().date)
        assertEquals(today, result.last().date)
    }

    @Test
    fun dailyStats_yearPeriod_leapYearHas366Days() {
        val leapNow = LocalDate.of(2024, 2, 29)
        val result = CalorieStats.computeDailyStats(emptyList(), leapNow, StatsPeriod.YEAR, zone)
        assertEquals(366, result.size)
        assertEquals(leapNow.minusDays(365), result.first().date)
    }

    @Test
    fun getPeriodRange_week_fullWeekMondayToSunday() {
        val (start, end) = CalorieStats.getPeriodRange(today, StatsPeriod.WEEK)
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertEquals(6, java.time.temporal.ChronoUnit.DAYS.between(start, end))
    }

    @Test
    fun getPeriodRange_month_startsOnFirst() {
        val (start, end) = CalorieStats.getPeriodRange(today, StatsPeriod.MONTH)
        assertEquals(1, start.dayOfMonth)
        assertEquals(today, end)
    }

    @Test
    fun getPeriodRange_year_rollingWindowEndsToday() {
        val (start, end) = CalorieStats.getPeriodRange(today, StatsPeriod.YEAR)
        assertEquals(today, end)
        assertEquals(today.minusDays(364), start)
    }

    @Test
    fun filterByEpochRange_includesOnlyDaysInsideRange() {
        val start = today.minusDays(3)
        val end = today.minusDays(1)
        val items = itemsOn(
            today.minusDays(5) to 500,
            today.minusDays(3) to 500,
            today.minusDays(1) to 500,
            today to 500
        )
        val result = CalorieStats.filterByEpochRange(
            items,
            start.atStartOfDay(zone).toInstant().toEpochMilli(),
            end.atStartOfDay(zone).toInstant().toEpochMilli(),
            zone
        )
        assertEquals(2, result.size)
    }

    @Test
    fun dailyCaloriesMap_groupsByDayAndSums() {
        val items = itemsOn(
            today to 500,
            today to 300,
            today.minusDays(1) to 400
        )
        val map = CalorieStats.dailyCaloriesMap(items, zone)
        assertEquals(2, map.size)
        assertEquals(800, map[today])
        assertEquals(400, map[today.minusDays(1)])
    }

    @Test
    fun dailyMacroMap_groupsByDayAndSums() {
        val items = itemsOnWithMacros(
            Triple(today, 500, Triple(30, 20, 50)),
            Triple(today, 300, Triple(10, 5, 40)),
            Triple(today.minusDays(1), 400, Triple(20, 10, 30))
        )
        val map = CalorieStats.dailyMacroMap(items, zone)
        assertEquals(2, map.size)
        assertEquals(Triple(40, 25, 90), map[today])
        assertEquals(Triple(20, 10, 30), map[today.minusDays(1)])
    }

    @Test
    fun dailyMacroMap_empty_returnsEmptyMap() {
        val map = CalorieStats.dailyMacroMap(emptyList(), zone)
        assertTrue(map.isEmpty())
    }

    @Test
    fun computeWeeklyStats_includesMacros() {
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = itemsOnWithMacros(
            Triple(currentWeekStart, 1000, Triple(50, 30, 100)),
            Triple(currentWeekStart.plusDays(2), 500, Triple(25, 15, 50))
        )
        val result = CalorieStats.computeWeeklyStats(items, today, StatsPeriod.MONTH, zone)
        assertEquals(4, result.size)
        assertEquals(75, result.last().totalProtein)
        assertEquals(45, result.last().totalFat)
        assertEquals(150, result.last().totalCarbs)
    }

    @Test
    fun computeDailyStats_includesMacros() {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val items = itemsOnWithMacros(
            Triple(monday, 1200, Triple(60, 40, 120))
        )
        val result = CalorieStats.computeDailyStats(items, today, StatsPeriod.WEEK, zone)
        assertEquals(7, result.size)
        assertEquals(60, result.first().protein)
        assertEquals(40, result.first().fat)
        assertEquals(120, result.first().carbs)
        assertEquals(0, result[1].protein)
    }
}
