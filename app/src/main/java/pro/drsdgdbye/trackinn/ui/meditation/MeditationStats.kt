package pro.drsdgdbye.trackinn.ui.meditation

import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.ui.stats.StatsPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Чистая логика статистики медитаций, независимая от ViewModel и Android.
 * Все функции принимают now/zone параметрами для детерминированного тестирования.
 */
internal object MeditationStats {

    fun filterSessionsByRange(
        sessions: List<MeditationSessionEntity>,
        start: LocalDate,
        end: LocalDate,
        zone: ZoneId
    ): List<MeditationSessionEntity> {
        return sessions.filter { session ->
            val sessionDate = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
            !sessionDate.isBefore(start) && !sessionDate.isAfter(end)
        }
    }

    fun filterSessionsByEpochRange(
        sessions: List<MeditationSessionEntity>,
        filterStart: Long,
        filterEnd: Long,
        zone: ZoneId
    ): List<MeditationSessionEntity> {
        val start = Instant.ofEpochMilli(filterStart).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(filterEnd).atZone(zone).toLocalDate()
        return filterSessionsByRange(sessions, start, end, zone)
    }

    fun getPeriodRange(now: LocalDate, period: StatsPeriod): Pair<LocalDate, LocalDate> {
        return when (period) {
            StatsPeriod.WEEK -> {
                val start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to now
            }
            StatsPeriod.MONTH -> {
                val start = now.withDayOfMonth(1)
                start to now
            }
            StatsPeriod.YEAR -> {
                val start = now.withDayOfYear(1)
                start to now
            }
        }
    }

    fun computeDashboardStats(
        sessions: List<MeditationSessionEntity>,
        today: LocalDate,
        zone: ZoneId
    ): DashboardStats {
        if (sessions.isEmpty()) return DashboardStats()
        val totalSessions = sessions.size
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val completed = sessions.count { it.wasCompleted }
        val completionRate = if (totalSessions > 0) (completed * 100 / totalSessions) else 0
        val streak = computeStreak(sessions, today, zone)
        return DashboardStats(totalSessions, totalMinutes, streak, completionRate)
    }

    fun computeStreak(
        sessions: List<MeditationSessionEntity>,
        today: LocalDate,
        zone: ZoneId
    ): Int {
        if (sessions.isEmpty()) return 0
        val sessionDates = sessions
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
        var streak = 0
        var expectedDate = today
        for (date in sessionDates) {
            if (date == expectedDate) {
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date == expectedDate.minusDays(1)) {
                // Allow gap if today hasn't been meditated yet
                expectedDate = date
                streak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    fun computeWeeklyStats(
        sessions: List<MeditationSessionEntity>,
        now: LocalDate,
        period: StatsPeriod,
        zone: ZoneId
    ): List<WeeklyStat> {
        return when (period) {
            StatsPeriod.WEEK -> {
                emptyList()
            }
            StatsPeriod.MONTH -> {
                val weeksCount = 4
                val result = mutableListOf<WeeklyStat>()
                for (i in (weeksCount - 1) downTo 0) {
                    val weekEnd = now.minusWeeks(i.toLong())
                    val weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    val weekEndClamped = weekEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    val minutes = sessions
                        .filter { session ->
                            val d = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
                            !d.isBefore(weekStart) && !d.isAfter(weekEndClamped)
                        }
                        .sumOf { it.durationMinutes }
                    result.add(WeeklyStat(weekStart, minutes))
                }
                result
            }
            StatsPeriod.YEAR -> {
                val result = mutableListOf<WeeklyStat>()
                for (i in 11 downTo 0) {
                    val monthStart = now.minusMonths(i.toLong()).withDayOfMonth(1)
                    val monthEnd = monthStart.plusMonths(1).minusDays(1)
                    val minutes = sessions
                        .filter { session ->
                            val d = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
                            !d.isBefore(monthStart) && !d.isAfter(monthEnd)
                        }
                        .sumOf { it.durationMinutes }
                    result.add(WeeklyStat(monthStart, minutes))
                }
                result
            }
        }
    }

    fun computeDailyStats(
        sessions: List<MeditationSessionEntity>,
        now: LocalDate,
        period: StatsPeriod,
        zone: ZoneId
    ): List<DailyStat> {
        val (start, end) = when (period) {
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
        val sessionMap = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
        }
        val result = mutableListOf<DailyStat>()
        var current = start
        while (!current.isAfter(end)) {
            val minutes = sessionMap[current]?.sumOf { it.durationMinutes } ?: 0
            result.add(DailyStat(current, minutes))
            current = current.plusDays(1)
        }
        return result
    }
}
