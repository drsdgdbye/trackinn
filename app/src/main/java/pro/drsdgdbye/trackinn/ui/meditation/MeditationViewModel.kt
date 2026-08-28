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
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import pro.drsdgdbye.trackinn.data.repository.SavedTimerRepository

enum class TimerState { IDLE, PREP, RUNNING, PAUSED, COMPLETED }

data class TimerUiState(
    val state: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val prepRemaining: Int = 0,
    val currentCheckpointIndex: Int = -1,
    val timer: SavedTimerEntity? = null
)

class MeditationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SavedTimerRepository
    private val db = TrackinnDatabase.getInstance(application)

    val timers = MutableStateFlow<List<SavedTimerEntity>>(emptyList())
    val sessions = MutableStateFlow<List<MeditationSessionEntity>>(emptyList())
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

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
        initSounds()
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
