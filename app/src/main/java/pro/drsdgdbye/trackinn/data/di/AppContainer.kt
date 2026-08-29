package pro.drsdgdbye.trackinn.data.di

import android.content.Context
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.repository.CompositeDishRepository
import pro.drsdgdbye.trackinn.data.repository.MealRepository
import pro.drsdgdbye.trackinn.data.repository.ProductRepository
import pro.drsdgdbye.trackinn.data.repository.SavedTimerRepository
import pro.drsdgdbye.trackinn.data.repository.TaskRepository
import pro.drsdgdbye.trackinn.data.repository.WeightEntryRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val db: TrackinnDatabase by lazy { TrackinnDatabase.getInstance(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val taskRepository: TaskRepository by lazy { TaskRepository(db.taskDao()) }

    val mealRepository: MealRepository by lazy { MealRepository(db.mealDao()) }

    val productRepository: ProductRepository by lazy { ProductRepository(db.productDao()) }

    val compositeDishRepository: CompositeDishRepository by lazy {
        CompositeDishRepository(db.compositeDishDao())
    }

    val savedTimerRepository: SavedTimerRepository by lazy {
        SavedTimerRepository(db.savedTimerDao(), db.meditationSessionDao())
    }

    val weightEntryRepository: WeightEntryRepository by lazy {
        WeightEntryRepository(db.weightEntryDao())
    }
}
