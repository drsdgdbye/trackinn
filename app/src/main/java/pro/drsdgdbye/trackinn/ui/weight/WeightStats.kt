package pro.drsdgdbye.trackinn.ui.weight

import java.time.DayOfWeek
import java.time.LocalDate

internal object WeightStats {

    fun computeDelta(current: Double, previous: Double?): Double? {
        if (previous == null) return null
        return current - previous
    }

    fun computeRemaining(current: Double, target: Double): Double? {
        if (target <= 0) return null
        return current - target
    }

    fun isWeighInToday(weighInDay: Int, today: LocalDate): Boolean {
        return today.dayOfWeek == dayOfWeekFor(weighInDay)
    }

    /** Ближайший день взвешивания, включая сегодняшний. */
    fun nextWeighInDate(weighInDay: Int, today: LocalDate): LocalDate {
        val target = dayOfWeekFor(weighInDay)
        var daysAhead = target.value - today.dayOfWeek.value
        if (daysAhead < 0) daysAhead += 7
        return today.plusDays(daysAhead.toLong())
    }

    private fun dayOfWeekFor(calendarDay: Int): DayOfWeek = when (calendarDay) {
        java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
        java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
        java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
        java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
        java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
        java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }
}
