package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.MealDao
import pro.drsdgdbye.trackinn.data.db.dao.MealItemWithDate
import pro.drsdgdbye.trackinn.data.db.entity.MealEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MealRepository(private val mealDao: MealDao) {

    fun getMealsByDate(date: Long): Flow<List<MealEntity>> = mealDao.getMealsByDate(startOfDay(date))

    fun getItemsByDate(date: Long): Flow<List<MealItemEntity>> = mealDao.getItemsByDate(startOfDay(date))

    fun getItemsByMealId(mealId: Long): Flow<List<MealItemEntity>> = mealDao.getItemsByMealId(mealId)

    fun getAllItemsWithDate(): Flow<List<MealItemWithDate>> = mealDao.getAllItemsWithDate()

    suspend fun getOrCreateMeal(date: Long, type: String): Long {
        val day = startOfDay(date)
        val existing = mealDao.getMealByDateAndType(day, type)
        return existing?.id ?: mealDao.insertOrReplace(MealEntity(type = type, date = day))
    }

    suspend fun addItem(mealId: Long, item: MealItemEntity): Long = mealDao.insertItem(item)

    suspend fun updateItem(item: MealItemEntity) = mealDao.updateItem(item)

    suspend fun deleteItem(itemId: Long) = mealDao.deleteItem(itemId)

    companion object {
        fun startOfDay(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
