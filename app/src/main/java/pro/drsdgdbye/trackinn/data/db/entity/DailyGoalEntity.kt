package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val date: Long,
    val caloriesGoal: Int
)
