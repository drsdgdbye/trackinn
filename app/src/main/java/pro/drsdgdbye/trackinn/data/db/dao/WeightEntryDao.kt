package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pro.drsdgdbye.trackinn.data.db.entity.WeightEntryEntity

@Dao
interface WeightEntryDao {
    @Query("SELECT * FROM weight_entries ORDER BY recordedAt DESC")
    fun getAll(): Flow<List<WeightEntryEntity>>

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Query("SELECT COUNT(*) FROM weight_entries")
    suspend fun getCount(): Int

    @Query("DELETE FROM weight_entries")
    suspend fun deleteAll()
}
