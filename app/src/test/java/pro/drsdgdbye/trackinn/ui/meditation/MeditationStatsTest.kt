package pro.drsdgdbye.trackinn.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class MeditationStatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Moscow")
    private val today: LocalDate = LocalDate.of(2026, 8, 28) // пятница

    private fun sessionOn(date: LocalDate, minutes: Int = 10, completed: Boolean = true): MeditationSessionEntity {
        val startedAt = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return MeditationSessionEntity(
            startedAt = startedAt,
            durationMinutes = minutes,
            completedAt = if (completed) startedAt + minutes * 60_000L else null,
            wasCompleted = completed
        )
    }

    @Test
    fun streak_emptySessions_returnsZero() {
        assertEquals(0, MeditationStats.computeStreak(emptyList(), today, zone))
    }

    @Test
    fun streak_sessionToday_returnsOne() {
        val sessions = listOf(sessionOn(today))
        assertEquals(1, MeditationStats.computeStreak(sessions, today, zone))
    }

    @Test
    fun streak_sessionTodayAndYesterday_returnsTwo() {
        val sessions = listOf(sessionOn(today), sessionOn(today.minusDays(1)))
        assertEquals(2, MeditationStats.computeStreak(sessions, today, zone))
    }

    @Test
    fun streak_gapTodayAllowed_continuesFromYesterday() {
        // Медитировал вчера и позавчера, сегодня ещё нет — стрик не обрывается
        val sessions = listOf(sessionOn(today.minusDays(1)), sessionOn(today.minusDays(2)))
        assertEquals(2, MeditationStats.computeStreak(sessions, today, zone))
    }

    @Test
    fun streak_breakInSequence_stopsAtBreak() {
        // вчера и 5 дней назад — стрик обрывается на вчерашнем дне
        val sessions = listOf(sessionOn(today.minusDays(1)), sessionOn(today.minusDays(5)))
        assertEquals(1, MeditationStats.computeStreak(sessions, today, zone))
    }

    @Test
    fun streak_duplicateDates_countedOnce() {
        val sessions = listOf(
            sessionOn(today, minutes = 10),
            sessionOn(today, minutes = 20),
            sessionOn(today.minusDays(1))
        )
        assertEquals(2, MeditationStats.computeStreak(sessions, today, zone))
    }

    @Test
    fun weeklyStats_monthPeriod_returnsFourWeeksWithSums() {
        val now = today
        val currentWeekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sessions = listOf(
            sessionOn(currentWeekStart, minutes = 5),
            sessionOn(currentWeekStart.plusDays(2), minutes = 15),
            sessionOn(currentWeekStart.minusDays(2), minutes = 30)
        )
        val result = MeditationStats.computeWeeklyStats(sessions, now, StatsPeriod.MONTH, zone)
        assertEquals(4, result.size)
        assertEquals(20, result.last().totalMinutes)
        assertEquals(30, result[result.size - 2].totalMinutes)
        assertEquals(0, result.first().totalMinutes)
    }

    @Test
    fun weeklyStats_weekPeriod_returnsEmpty() {
        val result = MeditationStats.computeWeeklyStats(emptyList(), today, StatsPeriod.WEEK, zone)
        assertTrue(result.isEmpty())
    }

    @Test
    fun dailyStats_weekPeriod_returnsSevenDaysStartingMonday() {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sessions = listOf(sessionOn(monday, minutes = 12), sessionOn(monday.plusDays(6), minutes = 8))
        val result = MeditationStats.computeDailyStats(sessions, today, StatsPeriod.WEEK, zone)
        assertEquals(7, result.size)
        assertEquals(monday, result.first().date)
        assertEquals(12, result.first().totalMinutes)
        assertEquals(8, result.last().totalMinutes)
    }

    @Test
    fun dailyStats_monthPeriod_spansFromFirstDayToNow() {
        val now = LocalDate.of(2026, 8, 28)
        val result = MeditationStats.computeDailyStats(emptyList(), now, StatsPeriod.MONTH, zone)
        assertEquals(28, result.size)
        assertEquals(LocalDate.of(2026, 8, 1), result.first().date)
        assertEquals(now, result.last().date)
    }

    @Test
    fun dailyStats_yearPeriod_leapYearHas366Days() {
        val leapNow = LocalDate.of(2024, 2, 29)
        val result = MeditationStats.computeDailyStats(emptyList(), leapNow, StatsPeriod.YEAR, zone)
        assertEquals(366, result.size)
        assertEquals(leapNow.minusDays(365), result.first().date)
    }

    @Test
    fun periodRange_week_startsOnMonday() {
        val now = LocalDate.of(2026, 8, 28) // пятница
        val (start, end) = MeditationStats.getPeriodRange(now, StatsPeriod.WEEK)
        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(now, end)
    }

    @Test
    fun periodRange_month_startsOnFirst() {
        val (start, end) = MeditationStats.getPeriodRange(today, StatsPeriod.MONTH)
        assertEquals(1, start.dayOfMonth)
        assertEquals(today, end)
    }

    @Test
    fun periodRange_year_startsOnJanFirst() {
        val (start, end) = MeditationStats.getPeriodRange(today, StatsPeriod.YEAR)
        assertEquals(LocalDate.of(2026, 1, 1), start)
        assertEquals(today, end)
    }

    @Test
    fun filterByEpochRange_includesOnlySessionsInsideRange() {
        val start = today.minusDays(3)
        val end = today.minusDays(1)
        val sessions = listOf(
            sessionOn(today.minusDays(5)),
            sessionOn(today.minusDays(3)),
            sessionOn(today.minusDays(1)),
            sessionOn(today)
        )
        val result = MeditationStats.filterSessionsByEpochRange(
            sessions,
            start.atStartOfDay(zone).toInstant().toEpochMilli(),
            end.atStartOfDay(zone).toInstant().toEpochMilli(),
            zone
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.startedAt <= end.atStartOfDay(zone).toInstant().toEpochMilli() })
        assertTrue(result.all { it.startedAt >= start.atStartOfDay(zone).toInstant().toEpochMilli() })
    }

    @Test
    fun dashboardStats_countsCompletedAndDuration() {
        val sessions = listOf(
            sessionOn(today, minutes = 15, completed = true),
            sessionOn(today.minusDays(1), minutes = 5, completed = false),
            sessionOn(today.minusDays(2), minutes = 10, completed = true)
        )
        val stats = MeditationStats.computeDashboardStats(sessions, today, zone)
        assertEquals(3, stats.totalSessions)
        assertEquals(30, stats.totalMinutes)
        assertEquals(66, stats.completionRate)
        assertEquals(3, stats.currentStreak)
    }
}
