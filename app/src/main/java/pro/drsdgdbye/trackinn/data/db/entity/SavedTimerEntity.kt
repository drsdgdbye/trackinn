package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_timers")
data class SavedTimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalMinutes: Int,
    val prepSeconds: Int = 0,
    val checkpointMinutes: String = "",
    val startSound: String? = null,
    val endSound: String? = null,
    val checkpointSound: String? = null,
    val timerProgressColor: String = "#4CAF50",
    val checkpointPassedColor: String = "#4CAF50",
    val checkpointPendingColor: String = "#9E9E9E",
    val position: Int
)
