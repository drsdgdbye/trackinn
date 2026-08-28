package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pro.drsdgdbye.trackinn.data.db.entity.DailyGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getGoalByDate(date: Long): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    suspend fun getGoalByDateSync(date: Long): DailyGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(goal: DailyGoalEntity)
}
