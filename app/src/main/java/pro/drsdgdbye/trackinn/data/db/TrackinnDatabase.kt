package pro.drsdgdbye.trackinn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import pro.drsdgdbye.trackinn.data.db.dao.CompositeDishDao
import pro.drsdgdbye.trackinn.data.db.dao.DailyGoalDao
import pro.drsdgdbye.trackinn.data.db.dao.MealDao
import pro.drsdgdbye.trackinn.data.db.dao.MeditationSessionDao
import pro.drsdgdbye.trackinn.data.db.dao.ProductDao
import pro.drsdgdbye.trackinn.data.db.dao.SavedTimerDao
import pro.drsdgdbye.trackinn.data.db.dao.TaskDao
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import pro.drsdgdbye.trackinn.data.db.entity.DailyGoalEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.db.entity.MeditationSessionEntity
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.db.entity.SavedTimerEntity
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ProductEntity::class,
        CompositeDishEntity::class,
        CompositeDishIngredientEntity::class,
        MealEntity::class,
        MealItemEntity::class,
        DailyGoalEntity::class,
        MeditationSessionEntity::class,
        SavedTimerEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrackinnDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun productDao(): ProductDao
    abstract fun compositeDishDao(): CompositeDishDao
    abstract fun mealDao(): MealDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun meditationSessionDao(): MeditationSessionDao
    abstract fun savedTimerDao(): SavedTimerDao

    companion object {
        @Volatile
        private var INSTANCE: TrackinnDatabase? = null

        fun getInstance(context: Context): TrackinnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackinnDatabase::class.java,
                    "trackinn.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
