package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.CompositeDishDao
import pro.drsdgdbye.trackinn.data.db.dao.DishWithIngredients
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import kotlinx.coroutines.flow.Flow

data class Nutrients(val calories: Int, val protein: Int, val fat: Int, val carbs: Int)

object NutrientCalculation {

    data class IngredientNutrients(
        val quantity: Int,
        val caloriesPer100: Int,
        val proteinPer100: Int,
        val fatPer100: Int,
        val carbsPer100: Int
    )

    fun totals(ingredients: List<IngredientNutrients>): Nutrients {
        var (c, p, f, cb) = listOf(0L, 0L, 0L, 0L)
        for (i in ingredients) {
            c += i.quantity.toLong() * i.caloriesPer100
            p += i.quantity.toLong() * i.proteinPer100
            f += i.quantity.toLong() * i.fatPer100
            cb += i.quantity.toLong() * i.carbsPer100
        }
        return Nutrients(
            (c / 100.0).toInt(),
            (p / 100.0).toInt(),
            (f / 100.0).toInt(),
            (cb / 100.0).toInt()
        )
    }

    fun per100(ingredients: List<IngredientNutrients>, cookedWeightGrams: Int): Nutrients {
        if (cookedWeightGrams <= 0) return Nutrients(0, 0, 0, 0)
        var (c, p, f, cb) = listOf(0L, 0L, 0L, 0L)
        for (i in ingredients) {
            c += i.quantity.toLong() * i.caloriesPer100
            p += i.quantity.toLong() * i.proteinPer100
            f += i.quantity.toLong() * i.fatPer100
            cb += i.quantity.toLong() * i.carbsPer100
        }
        return Nutrients(
            (c / cookedWeightGrams.toDouble()).toInt(),
            (p / cookedWeightGrams.toDouble()).toInt(),
            (f / cookedWeightGrams.toDouble()).toInt(),
            (cb / cookedWeightGrams.toDouble()).toInt()
        )
    }
}

class CompositeDishRepository(private val compositeDishDao: CompositeDishDao) {

    fun getAll(): Flow<List<CompositeDishEntity>> = compositeDishDao.getAll()

    fun search(query: String): Flow<List<CompositeDishEntity>> = compositeDishDao.search(query)

    suspend fun getById(id: Long): CompositeDishEntity? = compositeDishDao.getById(id)

    suspend fun getByName(name: String): CompositeDishEntity? = compositeDishDao.getByName(name)

    suspend fun create(dish: CompositeDishEntity, ingredients: List<CompositeDishIngredientEntity>): Long {
        val dishId = compositeDishDao.insert(dish)
        val ingredientsWithDishId = ingredients.map { it.copy(dishId = dishId) }
        compositeDishDao.insertIngredients(ingredientsWithDishId)
        return dishId
    }

    suspend fun update(dish: CompositeDishEntity, ingredients: List<CompositeDishIngredientEntity>) {
        compositeDishDao.update(dish)
        compositeDishDao.deleteAllIngredients(dish.id)
        val ingredientsWithDishId = ingredients.map { it.copy(dishId = dish.id) }
        compositeDishDao.insertIngredients(ingredientsWithDishId)
    }

    suspend fun delete(dish: CompositeDishEntity) = compositeDishDao.delete(dish)

    suspend fun getIngredientsList(dishId: Long): List<CompositeDishIngredientEntity> =
        compositeDishDao.getIngredientsList(dishId)
}
