package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationSessionDao {
    @Query("SELECT * FROM meditation_sessions ORDER BY startedAt DESC")
    fun getAll(): Flow<List<MeditationSessionEntity>>

    @Insert
    suspend fun insert(session: MeditationSessionEntity): Long

    @Update
    suspend fun update(session: MeditationSessionEntity)

    @Query("DELETE FROM meditation_sessions")
    suspend fun deleteAll()
}
