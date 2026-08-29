package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY position ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteCompleted()

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks")
    suspend fun getNextPosition(): Int
}
