package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.MeditationSessionDao
import pro.drsdgdbye.trackinn.data.db.dao.SavedTimerDao
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import kotlinx.coroutines.flow.Flow

class SavedTimerRepository(
    private val savedTimerDao: SavedTimerDao,
    private val meditationSessionDao: MeditationSessionDao
) {
    fun getAllTimers(): Flow<List<SavedTimerEntity>> = savedTimerDao.getAll()

    suspend fun getTimerById(id: Long): SavedTimerEntity? = savedTimerDao.getById(id)

    suspend fun createTimer(timer: SavedTimerEntity): Long {
        val position = savedTimerDao.getNextPosition()
        return savedTimerDao.insert(timer.copy(position = position))
    }

    suspend fun updateTimer(timer: SavedTimerEntity) = savedTimerDao.update(timer)

    suspend fun deleteTimer(timer: SavedTimerEntity) = savedTimerDao.delete(timer)

    fun getAllSessions(): Flow<List<MeditationSessionEntity>> = meditationSessionDao.getAll()

    suspend fun createSession(session: MeditationSessionEntity): Long = meditationSessionDao.insert(session)

    suspend fun updateSession(session: MeditationSessionEntity) = meditationSessionDao.update(session)

    suspend fun updatePositions(timers: List<SavedTimerEntity>) {
        timers.forEachIndexed { index, timer ->
            savedTimerDao.updatePosition(timer.id, index)
        }
    }
}
