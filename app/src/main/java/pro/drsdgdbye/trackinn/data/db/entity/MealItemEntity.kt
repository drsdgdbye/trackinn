package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CompositeDishEntity::class,
            parentColumns = ["id"],
            childColumns = ["compositeDishId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("mealId"),
        Index("productId"),
        Index("compositeDishId")
    ]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val productId: Long? = null,
    val compositeDishId: Long? = null,
    val name: String,
    val weight: Int,
    val calories: Int,
    val proteinPer100: Int = 0,
    val fatPer100: Int = 0,
    val carbsPer100: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val carbs: Int = 0
)
