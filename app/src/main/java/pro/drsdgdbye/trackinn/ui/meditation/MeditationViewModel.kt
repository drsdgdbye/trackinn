package pro.drsdgdbye.trackinn.ui.meditation

import android.app.Application
import android.media.SoundPool
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import pro.drsdgdbye.trackinn.data.repository.SavedTimerRepository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class TimerState { IDLE, PREP, RUNNING, PAUSED, COMPLETED }

enum class StatsPeriod { WEEK, MONTH, YEAR }

data class TimerUiState(
    val state: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val prepRemaining: Int = 0,
    val currentCheckpointIndex: Int = -1,
    val timer: SavedTimerEntity? = null
)

data class DashboardStats(
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val currentStreak: Int = 0,
    val completionRate: Int = 0
)

data class WeeklyStat(
    val weekStart: LocalDate,
    val totalMinutes: Int
)

data class DailyStat(
    val date: LocalDate,
    val totalMinutes: Int
)

class MeditationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavedTimerRepository
    private val db = TrackinnDatabase.getInstance(application)

    val timers = MutableStateFlow<List<SavedTimerEntity>>(emptyList())
    val sessions = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val selectedPeriod = MutableStateFlow(StatsPeriod.MONTH)
    val dashboardStats = MutableStateFlow(DashboardStats())
    val weeklyStats = MutableStateFlow<List<WeeklyStat>>(emptyList())
    val dailyStats = MutableStateFlow<List<DailyStat>>(emptyList())
    val filteredSessions = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())

    private var timerJob: Job? = null
    private var soundPool: SoundPool? = null
    private var startSoundId: Int = 0
    private var endSoundId: Int = 0
    private var checkpointSoundId: Int = 0
    private var sessionStartTime: Long = 0L

    init {
        repository = SavedTimerRepository(db.savedTimerDao(), db.meditationSessionDao())
        viewModelScope.launch {
            repository.getAllTimers().collect { timers.value = it }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions.value = it }
        }
        viewModelScope.launch {
            combine(sessions, selectedPeriod) { allSessions, period ->
                val now = LocalDate.now()
                val (start, end) = getPeriodRange(now, period)
                val zone = ZoneId.systemDefault()
                val filtered = allSessions.filter { session ->
                    val sessionDate = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
                    !sessionDate.isBefore(start) && !sessionDate.isAfter(end)
                }
                val stats = computeDashboardStats(allSessions)
                val weekly = computeWeeklyStats(allSessions, now, period)
                val daily = computeDailyStats(allSessions, now, period)
                Triple(stats, weekly, daily) to filtered
            }.collect { (statsPair, filtered) ->
                dashboardStats.value = statsPair.first
                weeklyStats.value = statsPair.second
                dailyStats.value = statsPair.third
                filteredSessions.value = filtered
            }
        }
        initSounds()
    }

    private fun getPeriodRange(now: LocalDate, period: StatsPeriod): Pair<LocalDate, LocalDate> {
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

    private fun computeDashboardStats(sessions: List<MeditationSessionEntity>): DashboardStats {
        if (sessions.isEmpty()) return DashboardStats()
        val totalSessions = sessions.size
        val totalMinutes = sessions.sumOf { it.durationMinutes }
        val completed = sessions.count { it.wasCompleted }
        val completionRate = if (totalSessions > 0) (completed * 100 / totalSessions) else 0
        val streak = computeStreak(sessions)
        return DashboardStats(totalSessions, totalMinutes, streak, completionRate)
    }

    private fun computeStreak(sessions: List<MeditationSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val sessionDates = sessions
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
            .distinct()
            .sortedDescending()
        val today = LocalDate.now()
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

    private fun computeWeeklyStats(
        sessions: List<MeditationSessionEntity>,
        now: LocalDate,
        period: StatsPeriod
    ): List<WeeklyStat> {
        val zone = ZoneId.systemDefault()
        val weeksCount = when (period) {
            StatsPeriod.WEEK -> 1
            StatsPeriod.MONTH -> 4
            StatsPeriod.YEAR -> 12
        }
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
        return result
    }

    private fun computeDailyStats(
        sessions: List<MeditationSessionEntity>,
        now: LocalDate,
        period: StatsPeriod
    ): List<DailyStat> {
        val zone = ZoneId.systemDefault()
        val daysCount = when (period) {
            StatsPeriod.WEEK -> 7
            StatsPeriod.MONTH -> now.lengthOfMonth()
            StatsPeriod.YEAR -> if (now.isLeapYear) 366 else 365
        }
        val start = now.minusDays((daysCount - 1).toLong())
        val sessionMap = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate()
        }
        val result = mutableListOf<DailyStat>()
        var current = start
        while (!current.isAfter(now)) {
            val minutes = sessionMap[current]?.sumOf { it.durationMinutes } ?: 0
            result.add(DailyStat(current, minutes))
            current = current.plusDays(1)
        }
        return result
    }

    fun setPeriod(period: StatsPeriod) {
        selectedPeriod.value = period
    }

    private fun initSounds() {
        val app = getApplication<Application>()
        soundPool = SoundPool.Builder().setMaxStreams(3).build()
        startSoundId = soundPool!!.load(app, R.raw.meditation_start, 1)
        endSoundId = soundPool!!.load(app, R.raw.meditation_end, 1)
        checkpointSoundId = soundPool!!.load(app, R.raw.meditation_checkpoint, 1)
    }

    fun startTimerById(id: Long) {
        viewModelScope.launch {
            val timer = repository.getTimerById(id)
            if (timer != null) {
                startTimer(timer)
            }
        }
    }

    fun startTimer(timer: SavedTimerEntity) {
        _uiState.value = TimerUiState(
            state = TimerState.PREP,
            prepRemaining = timer.prepSeconds,
            totalSeconds = timer.totalMinutes * 60,
            timer = timer
        )
        sessionStartTime = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            if (timer.prepSeconds > 0) {
                for (i in timer.prepSeconds downTo 1) {
                    _uiState.value = _uiState.value.copy(prepRemaining = i)
                    delay(1000)
                }
            }
            soundPool?.play(startSoundId, 1f, 1f, 1, 0, 1f)
            val totalSeconds = timer.totalMinutes * 60
            val checkpoints = timer.checkpointMinutes
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .sorted()
            var checkpointIdx = 0
            _uiState.value = _uiState.value.copy(
                state = TimerState.RUNNING,
                remainingSeconds = totalSeconds,
                currentCheckpointIndex = -1
            )
            for (i in totalSeconds downTo 1) {
                val state = _uiState.value.state
                if (state != TimerState.RUNNING) {
                    if (state == TimerState.PAUSED) {
                        delay(100)
                        continue
                    }
                    return@launch
                }
                _uiState.value = _uiState.value.copy(remainingSeconds = i)
                val elapsedMinutes = (totalSeconds - i) / 60
                if (checkpointIdx < checkpoints.size && elapsedMinutes >= checkpoints[checkpointIdx]) {
                    soundPool?.play(checkpointSoundId, 1f, 1f, 1, 0, 1f)
                    _uiState.value = _uiState.value.copy(currentCheckpointIndex = checkpointIdx)
                    checkpointIdx++
                }
                delay(1000)
            }
            soundPool?.play(endSoundId, 1f, 1f, 1, 0, 1f)
            _uiState.value = _uiState.value.copy(state = TimerState.COMPLETED)
            saveSession(completed = true)
        }
    }

    fun pauseTimer() {
        _uiState.value = _uiState.value.copy(state = TimerState.PAUSED)
    }

    fun resumeTimer() {
        _uiState.value = _uiState.value.copy(state = TimerState.RUNNING)
    }

    fun stopTimer() {
        timerJob?.cancel()
        saveSession(completed = false)
        _uiState.value = TimerUiState()
    }

    fun resetCompleted() {
        _uiState.value = TimerUiState()
    }

    fun createTimer(timer: SavedTimerEntity) {
        viewModelScope.launch {
            repository.createTimer(timer)
        }
    }

    fun updateTimer(timer: SavedTimerEntity) {
        viewModelScope.launch {
            repository.updateTimer(timer)
        }
    }

    fun deleteTimer(timer: SavedTimerEntity) {
        viewModelScope.launch {
            repository.deleteTimer(timer)
        }
    }

    fun updatePositions(timerList: List<SavedTimerEntity>) {
        viewModelScope.launch {
            repository.updatePositions(timerList)
        }
    }

    private fun saveSession(completed: Boolean) {
        val state = _uiState.value
        val timer = state.timer ?: return
        val elapsedSeconds = timer.totalMinutes * 60 - state.remainingSeconds
        viewModelScope.launch {
            repository.createSession(
                MeditationSessionEntity(
                    startedAt = sessionStartTime,
                    durationMinutes = elapsedSeconds / 60,
                    completedAt = if (completed) System.currentTimeMillis() else null,
                    wasCompleted = completed
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundPool?.release()
    }
}
