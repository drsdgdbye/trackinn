package pro.drsdgdbye.trackinn.ui.calorie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.dao.MealWithItems
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.repository.MealRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class CalorieViewModel(application: Application) : AndroidViewModel(application) {
    private val mealRepository: MealRepository
    private val settingsRepository = SettingsRepository(application)
    private val db = TrackinnDatabase.getInstance(application)

    val caloriesDailyGoal = settingsRepository.caloriesDailyGoal

    private val today: Long = MealRepository.startOfDay(System.currentTimeMillis())

    val meals: StateFlow<List<MealWithItems>>

    init {
        mealRepository = MealRepository(db.mealDao())
        meals = combine(
            mealRepository.getMealsByDate(today),
            mealRepository.getItemsByDate(today)
        ) { mealEntities, items ->
            mealEntities.map { meal ->
                MealWithItems(
                    meal = meal,
                    items = items.filter { it.mealId == meal.id }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addItemToMeal(mealType: String, name: String, weight: Int, calories: Int, productId: Long? = null, compositeDishId: Long? = null) {
        viewModelScope.launch {
            val mealId = mealRepository.getOrCreateMeal(today, mealType)
            mealRepository.addItem(
                mealId,
                MealItemEntity(
                    mealId = mealId,
                    productId = productId,
                    compositeDishId = compositeDishId,
                    name = name,
                    weight = weight,
                    calories = calories
                )
            )
        }
    }

    fun updateItem(item: MealItemEntity, newWeight: Int) {
        viewModelScope.launch {
            if (newWeight <= 0) return@launch
            val newCalories = if (item.weight > 0) {
                (item.calories.toLong() * newWeight / item.weight).toInt()
            } else item.calories
            mealRepository.updateItem(item.copy(weight = newWeight, calories = newCalories))
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            mealRepository.deleteItem(itemId)
        }
    }
}
