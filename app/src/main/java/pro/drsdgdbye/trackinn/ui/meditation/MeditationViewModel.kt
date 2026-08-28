package pro.drsdgdbye.trackinn.ui.meditation

import android.app.Application
import android.media.SoundPool
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.SavedTimerRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

class MeditationViewModel(
    private val application: Application,
    private val repository: SavedTimerRepository
) : ViewModel() {

    val timers = MutableStateFlow<List<SavedTimerEntity>>(emptyList())
    val sessions = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val selectedPeriod = MutableStateFlow(StatsPeriod.MONTH)
    val dateFilter = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dashboardStats = MutableStateFlow(DashboardStats())
    val weeklyStats = MutableStateFlow<List<WeeklyStat>>(emptyList())
    val dailyStats = MutableStateFlow<List<DailyStat>>(emptyList())
    val filteredSessions = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())

    private val _soundsReady = MutableStateFlow(false)
    val soundsReady: StateFlow<Boolean> = _soundsReady.asStateFlow()

    private var timerJob: Job? = null
    private var soundPool: SoundPool? = null
    private var soundMap = mutableMapOf<String, Int>()
    private var loadedSoundsCount = 0
    private var sessionStartTime: Long = 0L

    init {
        viewModelScope.launch {
            repository.getAllTimers().collect { timerList ->
                val migrated = mutableListOf<SavedTimerEntity>()
                for (timer in timerList) {
                    migrated.add(migrateTimerIfNeeded(timer))
                }
                timers.value = migrated
            }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions.value = it }
        }
        viewModelScope.launch {
            combine(sessions, selectedPeriod, dateFilter) { allSessions, period, (filterStart, filterEnd) ->
                val now = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val filtered = if (filterStart != null && filterEnd != null) {
                    MeditationStats.filterSessionsByEpochRange(allSessions, filterStart, filterEnd, zone)
                } else {
                    val (start, end) = MeditationStats.getPeriodRange(now, period)
                    MeditationStats.filterSessionsByRange(allSessions, start, end, zone)
                }
                val stats = MeditationStats.computeDashboardStats(allSessions, now, zone)
                val weekly = MeditationStats.computeWeeklyStats(allSessions, now, period, zone)
                val daily = MeditationStats.computeDailyStats(allSessions, now, period, zone)
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

    private val soundMigrationMap = mapOf(
        "plink" to "meditation_start",
        "bell" to "meditation_end",
        "chime" to "meditation_checkpoint",
        "gong" to "meditation_checkpoint",
        "drop" to "meditation_checkpoint"
    )

    private suspend fun migrateTimerIfNeeded(timer: SavedTimerEntity): SavedTimerEntity {
        var updated = timer
        val newStartSound = soundMigrationMap[timer.startSound] ?: timer.startSound
        val newEndSound = soundMigrationMap[timer.endSound] ?: timer.endSound
        val newCheckpointSound = soundMigrationMap[timer.checkpointSound] ?: timer.checkpointSound
        if (newStartSound != timer.startSound || newEndSound != timer.endSound || newCheckpointSound != timer.checkpointSound) {
            updated = timer.copy(
                startSound = newStartSound,
                endSound = newEndSound,
                checkpointSound = newCheckpointSound
            )
            repository.updateTimer(updated)
        }
        return updated
    }

    fun setPeriod(period: StatsPeriod) {
        selectedPeriod.value = period
    }

    fun setDateFilter(start: Long?, end: Long?) {
        dateFilter.value = start to end
    }

    private fun initSounds() {
        soundPool = SoundPool.Builder().setMaxStreams(3).build()
        soundPool?.setOnLoadCompleteListener { _, _, _ ->
            loadedSoundsCount++
            if (loadedSoundsCount >= 3) {
                _soundsReady.value = true
            }
        }
        soundMap["meditation_start"] = soundPool!!.load(application, R.raw.meditation_start, 1)
        soundMap["meditation_end"] = soundPool!!.load(application, R.raw.meditation_end, 1)
        soundMap["meditation_checkpoint"] = soundPool!!.load(application, R.raw.meditation_checkpoint, 1)
    }

    private fun playSound(soundName: String?) {
        if (!_soundsReady.value) return
        val id = soundMap[soundName] ?: soundMap["meditation_start"] ?: return
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
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
            // Wait for sounds to be ready before playing start sound
            if (!_soundsReady.value) {
                soundsReady.filter { it }.first()
            }
            playSound(timer.startSound)
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
                    playSound(timer.checkpointSound)
                    _uiState.value = _uiState.value.copy(currentCheckpointIndex = checkpointIdx)
                    checkpointIdx++
                }
                delay(1000)
            }
            playSound(timer.endSound)
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("MeditationViewModel requires an Application")
                MeditationViewModel(
                    app,
                    appContainer().savedTimerRepository
                )
            }
        }
    }
}
