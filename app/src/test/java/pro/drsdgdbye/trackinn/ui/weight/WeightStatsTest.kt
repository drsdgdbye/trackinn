package pro.drsdgdbye.trackinn.ui.weight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightStatsTest {

    @Test
    fun computeDelta_returnsDifference() {
        assertEquals(-0.5, WeightStats.computeDelta(70.5, 71.0)!!, 0.001)
        assertEquals(0.5, WeightStats.computeDelta(71.0, 70.5)!!, 0.001)
    }

    @Test
    fun computeDelta_noPrevious_returnsNull() {
        assertNull(WeightStats.computeDelta(70.5, null))
    }

    @Test
    fun computeRemaining_targetZero_returnsNull() {
        assertNull(WeightStats.computeRemaining(70.5, 0.0))
    }

    @Test
    fun computeRemaining_positiveRemaining() {
        assertEquals(5.5, WeightStats.computeRemaining(70.5, 65.0)!!, 0.001)
    }

    @Test
    fun computeRemaining_reachedTarget_negative() {
        assertEquals(-2.0, WeightStats.computeRemaining(63.0, 65.0)!!, 0.001)
    }

    @Test
    fun isWeighInToday_sundayDefault() {
        val sunday = LocalDate.of(2026, 8, 30)
        assertTrue(WeightStats.isWeighInToday(java.util.Calendar.SUNDAY, sunday))
        assertFalse(WeightStats.isWeighInToday(java.util.Calendar.MONDAY, sunday))
    }

    @Test
    fun nextWeighInDate_sameDay_returnsToday() {
        val sunday = LocalDate.of(2026, 8, 30)
        assertEquals(sunday, WeightStats.nextWeighInDate(java.util.Calendar.SUNDAY, sunday))
    }

    @Test
    fun nextWeighInDate_monday_sundayIsToday() {
        val sunday = LocalDate.of(2026, 8, 30)
        assertEquals(sunday, WeightStats.nextWeighInDate(java.util.Calendar.SUNDAY, sunday))
    }

    @Test
    fun nextWeighInDate_nextWeekday_rollsForward() {
        val friday = LocalDate.of(2026, 8, 28)
        val expectedSunday = LocalDate.of(2026, 8, 30)
        assertEquals(expectedSunday, WeightStats.nextWeighInDate(java.util.Calendar.SUNDAY, friday))
    }
}
