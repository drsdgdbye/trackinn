package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import kotlinx.coroutines.flow.Flow

data class DishWithIngredients(
    val dish: CompositeDishEntity,
    val ingredients: List<CompositeDishIngredientEntity>
)

@Dao
interface CompositeDishDao {
    @Query("SELECT * FROM composite_dishes ORDER BY dishType, name ASC")
    fun getAll(): Flow<List<CompositeDishEntity>>

    @Query("SELECT * FROM composite_dishes WHERE name LIKE :query || '%' COLLATE NOCASE ORDER BY name ASC")
    fun search(query: String): Flow<List<CompositeDishEntity>>

    @Query("SELECT * FROM composite_dishes WHERE id = :id")
    suspend fun getById(id: Long): CompositeDishEntity?

    @Query("SELECT * FROM composite_dishes WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CompositeDishEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(dish: CompositeDishEntity): Long

    @Update
    suspend fun update(dish: CompositeDishEntity)

    @Delete
    suspend fun delete(dish: CompositeDishEntity)

    @Query("DELETE FROM composite_dishes")
    suspend fun deleteAll()

    @Query("DELETE FROM composite_dish_ingredients")
    suspend fun deleteAllIngredientsGlobal()

    @Query("SELECT * FROM composite_dish_ingredients WHERE dishId = :dishId ORDER BY position ASC")
    fun getIngredients(dishId: Long): Flow<List<CompositeDishIngredientEntity>>

    @Query("SELECT * FROM composite_dish_ingredients WHERE dishId = :dishId ORDER BY position ASC")
    suspend fun getIngredientsList(dishId: Long): List<CompositeDishIngredientEntity>

    @Insert
    suspend fun insertIngredient(ingredient: CompositeDishIngredientEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<CompositeDishIngredientEntity>)

    @Delete
    suspend fun deleteIngredient(ingredient: CompositeDishIngredientEntity)

    @Query("DELETE FROM composite_dish_ingredients WHERE dishId = :dishId")
    suspend fun deleteAllIngredients(dishId: Long)
}
