package pro.drsdgdbye.trackinn.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import pro.drsdgdbye.trackinn.data.db.dao.CompositeDishDao
import pro.drsdgdbye.trackinn.data.db.dao.MealDao
import pro.drsdgdbye.trackinn.data.db.dao.MeditationSessionDao
import pro.drsdgdbye.trackinn.data.db.dao.ProductDao
import pro.drsdgdbye.trackinn.data.db.dao.SavedTimerDao
import pro.drsdgdbye.trackinn.data.db.dao.TaskDao
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
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
        MeditationSessionEntity::class,
        SavedTimerEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrackinnDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun productDao(): ProductDao
    abstract fun compositeDishDao(): CompositeDishDao
    abstract fun mealDao(): MealDao
    abstract fun meditationSessionDao(): MeditationSessionDao
    abstract fun savedTimerDao(): SavedTimerDao

    companion object {
        @Volatile
        private var INSTANCE: TrackinnDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS daily_goals")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE meal_items_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "mealId INTEGER NOT NULL, " +
                        "productId INTEGER, " +
                        "compositeDishId INTEGER, " +
                        "name TEXT NOT NULL, " +
                        "weight INTEGER NOT NULL, " +
                        "calories INTEGER NOT NULL, " +
                        "FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE SET NULL, " +
                        "FOREIGN KEY(compositeDishId) REFERENCES composite_dishes(id) ON UPDATE NO ACTION ON DELETE SET NULL)"
                )
                db.execSQL(
                    "INSERT INTO meal_items_new (id, mealId, productId, compositeDishId, name, weight, calories) " +
                        "SELECT id, mealId, productId, compositeDishId, name, weight, calories FROM meal_items"
                )
                db.execSQL("DROP TABLE meal_items")
                db.execSQL("ALTER TABLE meal_items_new RENAME TO meal_items")
                db.execSQL("CREATE INDEX index_meal_items_mealId ON meal_items (mealId)")
                db.execSQL("CREATE INDEX index_meal_items_productId ON meal_items (productId)")
                db.execSQL("CREATE INDEX index_meal_items_compositeDishId ON meal_items (compositeDishId)")

                db.execSQL(
                    "CREATE TABLE composite_dish_ingredients_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "dishId INTEGER NOT NULL, " +
                        "productId INTEGER, " +
                        "quantity INTEGER NOT NULL, " +
                        "position INTEGER NOT NULL, " +
                        "FOREIGN KEY(dishId) REFERENCES composite_dishes(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE SET NULL)"
                )
                db.execSQL(
                    "INSERT INTO composite_dish_ingredients_new (id, dishId, productId, quantity, position) " +
                        "SELECT id, dishId, productId, quantity, position FROM composite_dish_ingredients"
                )
                db.execSQL("DROP TABLE composite_dish_ingredients")
                db.execSQL("ALTER TABLE composite_dish_ingredients_new RENAME TO composite_dish_ingredients")
                db.execSQL("CREATE INDEX index_composite_dish_ingredients_dishId ON composite_dish_ingredients (dishId)")
                db.execSQL("CREATE INDEX index_composite_dish_ingredients_productId ON composite_dish_ingredients (productId)")
            }
        }

        fun getInstance(context: Context): TrackinnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackinnDatabase::class.java,
                    "trackinn.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
