package pro.drsdgdbye.trackinn.data.export

import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity

data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val products: List<ProductEntity> = emptyList(),
    val compositeDishes: List<CompositeDishEntity> = emptyList(),
    val compositeDishIngredients: List<CompositeDishIngredientEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val mealItems: List<MealItemEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val savedTimers: List<SavedTimerEntity> = emptyList(),
    val meditationSessions: List<MeditationSessionEntity> = emptyList(),
    val settings: Map<String, Any?> = emptyMap()
)
