package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String? = null,
    val unit: String = "GRAM",
    val caloriesPer100: Int,
    val proteinPer100: Int,
    val fatPer100: Int,
    val carbsPer100: Int,
    val lastModified: Long = System.currentTimeMillis()
)
