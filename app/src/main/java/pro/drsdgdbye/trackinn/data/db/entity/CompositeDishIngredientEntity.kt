package pro.drsdgdbye.trackinn.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "composite_dish_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = CompositeDishEntity::class,
            parentColumns = ["id"],
            childColumns = ["dishId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"]
        )
    ],
    indices = [
        Index("dishId"),
        Index("productId")
    ]
)
data class CompositeDishIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dishId: Long,
    val productId: Long,
    val quantity: Int,
    val position: Int
)
