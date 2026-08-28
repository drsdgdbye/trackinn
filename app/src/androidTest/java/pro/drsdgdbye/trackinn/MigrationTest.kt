package pro.drsdgdbye.trackinn

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrate1To2_dropsDailyGoalsAndPreservesUserData() {
        createDatabaseV1().use { db ->
            db.execSQL(
                "INSERT INTO products (name, category, unit, caloriesPer100, proteinPer100, fatPer100, carbsPer100) " +
                    "VALUES ('Oatmeal', 'cereal', 'GRAM', 350, 12, 6, 60)"
            )
            db.execSQL("INSERT INTO meals (type, date) VALUES ('BREAKFAST', 1750000000000)")
            db.execSQL(
                "INSERT INTO meal_items (mealId, name, weight, calories) " +
                    "VALUES (1, 'Oatmeal', 100, 350)"
            )
            db.execSQL(
                "INSERT INTO tasks (title, isDone, position, createdAt, updatedAt) " +
                    "VALUES ('Buy milk', 0, 1, 1750000000000, 1750000000000)"
            )
            db.execSQL(
                "INSERT INTO meditation_sessions (startedAt, durationMinutes, wasCompleted) " +
                    "VALUES (1750000000000, 10, 1)"
            )
            db.execSQL("INSERT INTO daily_goals (date, caloriesGoal) VALUES (1750000000000, 2000)")
        }

        val roomDb = Room.databaseBuilder(context, TrackinnDatabase::class.java, TEST_DB)
            .addMigrations(TrackinnDatabase.MIGRATION_1_2)
            .build()

        val products = runBlocking { roomDb.productDao().getAll().first() }
        assertEquals(1, products.size)
        assertEquals("Oatmeal", products[0].name)

        val meals = runBlocking { roomDb.mealDao().getAll().first() }
        assertEquals(1, meals.size)
        val items = runBlocking { roomDb.mealDao().getItemsByMealIdList(meals[0].id) }
        assertEquals(1, items.size)
        assertEquals(350, items[0].calories)

        val tasks = runBlocking { roomDb.taskDao().getAll().first() }
        assertEquals(1, tasks.size)
        assertEquals("Buy milk", tasks[0].title)

        val sessions = runBlocking { roomDb.meditationSessionDao().getAll().first() }
        assertEquals(1, sessions.size)
        assertEquals(10, sessions[0].durationMinutes)

        roomDb.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'daily_goals'"
        ).use { cursor ->
            assertTrue("daily_goals must be dropped", !cursor.moveToFirst())
        }
        roomDb.close()
    }

    private fun createDatabaseV1(): SupportSQLiteDatabase {
        val openHelper: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS tasks (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "title TEXT NOT NULL, " +
                                "isDone INTEGER NOT NULL, " +
                                "dueDate INTEGER, " +
                                "dueTime INTEGER, " +
                                "position INTEGER NOT NULL, " +
                                "createdAt INTEGER NOT NULL, " +
                                "updatedAt INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS products (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "name TEXT NOT NULL, " +
                                "category TEXT, " +
                                "unit TEXT NOT NULL, " +
                                "caloriesPer100 INTEGER NOT NULL, " +
                                "proteinPer100 INTEGER NOT NULL, " +
                                "fatPer100 INTEGER NOT NULL, " +
                                "carbsPer100 INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS composite_dishes (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "name TEXT NOT NULL, " +
                                "dishType TEXT NOT NULL, " +
                                "cookedWeightGrams INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS composite_dish_ingredients (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "dishId INTEGER NOT NULL, " +
                                "productId INTEGER NOT NULL, " +
                                "quantity INTEGER NOT NULL, " +
                                "position INTEGER NOT NULL, " +
                                "FOREIGN KEY(dishId) REFERENCES composite_dishes(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                                "FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE NO ACTION)"
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_composite_dish_ingredients_dishId ON composite_dish_ingredients (dishId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_composite_dish_ingredients_productId ON composite_dish_ingredients (productId)")
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS meals (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "type TEXT NOT NULL, " +
                                "date INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS meal_items (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "mealId INTEGER NOT NULL, " +
                                "productId INTEGER, " +
                                "compositeDishId INTEGER, " +
                                "name TEXT NOT NULL, " +
                                "weight INTEGER NOT NULL, " +
                                "calories INTEGER NOT NULL, " +
                                "FOREIGN KEY(mealId) REFERENCES meals(id) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                                "FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE NO ACTION, " +
                                "FOREIGN KEY(compositeDishId) REFERENCES composite_dishes(id) ON UPDATE NO ACTION ON DELETE NO ACTION)"
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_mealId ON meal_items (mealId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_productId ON meal_items (productId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_items_compositeDishId ON meal_items (compositeDishId)")
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS daily_goals (" +
                                "date INTEGER NOT NULL, " +
                                "caloriesGoal INTEGER NOT NULL, " +
                                "PRIMARY KEY(date))"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS meditation_sessions (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "startedAt INTEGER NOT NULL, " +
                                "durationMinutes INTEGER NOT NULL, " +
                                "completedAt INTEGER, " +
                                "wasCompleted INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS saved_timers (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "name TEXT NOT NULL, " +
                                "totalMinutes INTEGER NOT NULL, " +
                                "prepSeconds INTEGER NOT NULL, " +
                                "checkpointMinutes TEXT NOT NULL, " +
                                "startSound TEXT, " +
                                "endSound TEXT, " +
                                "checkpointSound TEXT, " +
                                "timerProgressColor TEXT NOT NULL, " +
                                "checkpointPassedColor TEXT NOT NULL, " +
                                "checkpointPendingColor TEXT NOT NULL, " +
                                "position INTEGER NOT NULL)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

                    override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        return openHelper.writableDatabase
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
    }
}
