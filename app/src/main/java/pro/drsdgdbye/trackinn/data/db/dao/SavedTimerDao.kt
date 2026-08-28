package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTimerDao {
    @Query("SELECT * FROM saved_timers ORDER BY position ASC")
    fun getAll(): Flow<List<SavedTimerEntity>>

    @Query("SELECT * FROM saved_timers WHERE id = :id")
    suspend fun getById(id: Long): SavedTimerEntity?

    @Insert
    suspend fun insert(timer: SavedTimerEntity): Long

    @Update
    suspend fun update(timer: SavedTimerEntity)

    @Delete
    suspend fun delete(timer: SavedTimerEntity)

    @Query("DELETE FROM saved_timers")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM saved_timers")
    suspend fun getNextPosition(): Int

    @Query("UPDATE saved_timers SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}
