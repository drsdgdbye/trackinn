package pro.drsdgdbye.trackinn.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pro.drsdgdbye.trackinn.data.db.entity.MealEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow

data class MealWithItems(
    val meal: MealEntity,
    val items: List<MealItemEntity>
)

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY date ASC, type ASC")
    fun getAll(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY type ASC")
    fun getMealsByDate(date: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date = :date AND type = :type LIMIT 1")
    suspend fun getMealByDateAndType(date: Long, type: String): MealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(meal: MealEntity): Long

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY id ASC")
    fun getItemsByMealId(mealId: Long): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM meal_items WHERE mealId = :mealId ORDER BY id ASC")
    suspend fun getItemsByMealIdList(mealId: Long): List<MealItemEntity>

    @Query("SELECT meal_items.* FROM meal_items INNER JOIN meals ON meal_items.mealId = meals.id WHERE meals.date = :date ORDER BY meal_items.id ASC")
    fun getItemsByDate(date: Long): Flow<List<MealItemEntity>>

    @Insert
    suspend fun insertItem(item: MealItemEntity): Long

    @Update
    suspend fun updateItem(item: MealItemEntity)

    @Query("DELETE FROM meal_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    suspend fun deleteAllItems(mealId: Long)

    @Query("DELETE FROM meals")
    suspend fun deleteAll()

    @Query("DELETE FROM meal_items")
    suspend fun deleteAllItemsGlobal()
}
