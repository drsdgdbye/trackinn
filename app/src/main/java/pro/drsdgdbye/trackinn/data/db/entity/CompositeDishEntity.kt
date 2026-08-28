package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "composite_dishes")
data class CompositeDishEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dishType: String,
    val cookedWeightGrams: Int
)
