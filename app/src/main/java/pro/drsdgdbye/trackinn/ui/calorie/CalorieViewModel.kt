package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.dao.MealWithItems
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.MealRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class CalorieViewModel(
    private val mealRepository: MealRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val caloriesDailyGoal = settingsRepository.caloriesDailyGoal
    val progressBarColor = settingsRepository.progressBarColor
    val approachingGoalColor = settingsRepository.approachingGoalColor
    val exceedingGoalColor = settingsRepository.exceedingGoalColor

    private val today: Long = MealRepository.startOfDay(System.currentTimeMillis())

    val meals: StateFlow<List<MealWithItems>> = combine(
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                CalorieViewModel(container.mealRepository, container.settingsRepository)
            }
        }
    }
}
