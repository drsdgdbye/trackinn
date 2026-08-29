package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val recordedAt: Long = System.currentTimeMillis()
)
