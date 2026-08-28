package pro.drsdgdbye.trackinn.data.export

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.settings.SettingsKeys
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository
import pro.drsdgdbye.trackinn.data.settings.ThemeMode

class ExportImportManager(private val context: Context) {
    private val db = TrackinnDatabase.getInstance(context)
    private val settingsRepository = SettingsRepository(context)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val products = db.productDao().getAll().first()
        val dishes = db.compositeDishDao().getAll().first()
        val allIngredients = mutableListOf<CompositeDishIngredientEntity>()
        for (dish in dishes) {
            allIngredients.addAll(db.compositeDishDao().getIngredientsList(dish.id))
        }
        val meals = db.mealDao().getAll().first()
        val allMealItems = mutableListOf<MealItemEntity>()
        for (meal in meals) {
            allMealItems.addAll(db.mealDao().getItemsByMealIdList(meal.id))
        }
        val tasks = db.taskDao().getAll().first()
        val savedTimers = db.savedTimerDao().getAll().first()
        val meditationSessions = db.meditationSessionDao().getAll().first()

        val settings = mapOf(
            "calories_daily_goal" to settingsRepository.caloriesDailyGoal.first(),
            "theme" to settingsRepository.theme.first().name,
            "language" to settingsRepository.language.first(),
            "module_todo" to settingsRepository.moduleTodo.first(),
            "module_calories" to settingsRepository.moduleCalories.first(),
            "module_meditation" to settingsRepository.moduleMeditation.first(),
            "completed_task_color" to settingsRepository.completedTaskColor.first(),
            "deadline_safe_color" to settingsRepository.deadlineSafeColor.first(),
            "deadline_warning_color" to settingsRepository.deadlineWarningColor.first(),
            "deadline_danger_color" to settingsRepository.deadlineDangerColor.first(),
            "progress_bar_color" to settingsRepository.progressBarColor.first(),
            "approaching_goal_color" to settingsRepository.approachingGoalColor.first(),
            "exceeding_goal_color" to settingsRepository.exceedingGoalColor.first()
        )

        val exportData = ExportData(
            products = products,
            compositeDishes = dishes,
            compositeDishIngredients = allIngredients,
            meals = meals,
            mealItems = allMealItems,
            tasks = tasks,
            savedTimers = savedTimers,
            meditationSessions = meditationSessions,
            settings = settings
        )

        gson.toJson(exportData)
    }

    suspend fun importFromJson(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<ExportData>() {}.type
            val data: ExportData = gson.fromJson(json, type)

            db.withTransaction {
                db.compositeDishDao().deleteAllIngredientsGlobal()
                db.mealDao().deleteAllItemsGlobal()
                db.productDao().deleteAll()
                db.compositeDishDao().deleteAll()
                db.mealDao().deleteAll()
                db.taskDao().deleteAll()
                db.savedTimerDao().deleteAll()
                db.meditationSessionDao().deleteAll()

                for (product in data.products) {
                    db.productDao().insert(product.copy(id = 0))
                }
                val oldToNewDishIds = mutableMapOf<Long, Long>()
                for (dish in data.compositeDishes) {
                    val newId = db.compositeDishDao().insert(dish.copy(id = 0))
                    oldToNewDishIds[dish.id] = newId
                }
                for (ingredient in data.compositeDishIngredients) {
                    val newDishId = oldToNewDishIds[ingredient.dishId] ?: continue
                    db.compositeDishDao().insertIngredient(ingredient.copy(id = 0, dishId = newDishId))
                }

                val oldToNewMealIds = mutableMapOf<Long, Long>()
                for (meal in data.meals) {
                    val newId = db.mealDao().insertOrReplace(meal.copy(id = 0))
                    oldToNewMealIds[meal.id] = newId
                }
                for (item in data.mealItems) {
                    val newMealId = oldToNewMealIds[item.mealId] ?: continue
                    db.mealDao().insertItem(item.copy(id = 0, mealId = newMealId))
                }

                for (task in data.tasks) {
                    db.taskDao().insert(task.copy(id = 0))
                }
                for (timer in data.savedTimers) {
                    db.savedTimerDao().insert(timer.copy(id = 0))
                }
                for (session in data.meditationSessions) {
                    db.meditationSessionDao().insert(session.copy(id = 0))
                }
            }

            data.settings["calories_daily_goal"]?.let {
                settingsRepository.setCaloriesDailyGoal((it as Double).toInt())
            }
            data.settings["theme"]?.let {
                settingsRepository.setTheme(ThemeMode.valueOf(it as String))
            }
            data.settings["language"]?.let {
                settingsRepository.setLanguage(it as? String)
            }
            data.settings["module_todo"]?.let {
                settingsRepository.setModuleEnabled("todo", it as Boolean)
            }
            data.settings["module_calories"]?.let {
                settingsRepository.setModuleEnabled("calories", it as Boolean)
            }
            data.settings["module_meditation"]?.let {
                settingsRepository.setModuleEnabled("meditation", it as Boolean)
            }
            data.settings["completed_task_color"]?.let {
                settingsRepository.setColor(SettingsKeys.COMPLETED_TASK_COLOR, it as String)
            }
            data.settings["deadline_safe_color"]?.let {
                settingsRepository.setColor(SettingsKeys.DEADLINE_SAFE_COLOR, it as String)
            }
            data.settings["deadline_warning_color"]?.let {
                settingsRepository.setColor(SettingsKeys.DEADLINE_WARNING_COLOR, it as String)
            }
            data.settings["deadline_danger_color"]?.let {
                settingsRepository.setColor(SettingsKeys.DEADLINE_DANGER_COLOR, it as String)
            }
            data.settings["progress_bar_color"]?.let {
                settingsRepository.setColor(SettingsKeys.PROGRESS_BAR_COLOR, it as String)
            }
            data.settings["approaching_goal_color"]?.let {
                settingsRepository.setColor(SettingsKeys.APPROACHING_GOAL_COLOR, it as String)
            }
            data.settings["exceeding_goal_color"]?.let {
                settingsRepository.setColor(SettingsKeys.EXCEEDING_GOAL_COLOR, it as String)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importProductsFromJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val type = object : TypeToken<List<ProductEntity>>() {}.type
            val products: List<ProductEntity> = gson.fromJson(json, type)
            var count = 0
            for (product in products) {
                db.productDao().insert(product.copy(id = 0))
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
