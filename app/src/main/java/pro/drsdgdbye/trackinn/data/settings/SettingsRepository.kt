package pro.drsdgdbye.trackinn.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val CALORIES_DAILY_GOAL = intPreferencesKey("calories_daily_goal")
    val THEME = stringPreferencesKey("theme")
    val LANGUAGE = stringPreferencesKey("language")
    val MODULE_TODO = booleanPreferencesKey("module_todo")
    val MODULE_CALORIES = booleanPreferencesKey("module_calories")
    val MODULE_MEDITATION = booleanPreferencesKey("module_meditation")

    val WEIGHT_TARGET = floatPreferencesKey("weight_target")
    val WEIGHT_WEIGH_IN_DAY = intPreferencesKey("weight_weigh_in_day")
    val MODULE_WEIGHT = booleanPreferencesKey("module_weight")

    val COMPLETED_TASK_COLOR = stringPreferencesKey("completed_task_color")
    val DEADLINE_SAFE_COLOR = stringPreferencesKey("deadline_safe_color")
    val DEADLINE_WARNING_COLOR = stringPreferencesKey("deadline_warning_color")
    val DEADLINE_DANGER_COLOR = stringPreferencesKey("deadline_danger_color")

    val PROGRESS_BAR_COLOR = stringPreferencesKey("progress_bar_color")
    val APPROACHING_GOAL_COLOR = stringPreferencesKey("approaching_goal_color")
    val EXCEEDING_GOAL_COLOR = stringPreferencesKey("exceeding_goal_color")
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class SettingsRepository(private val context: Context) {

    val caloriesDailyGoal: Flow<Int> = context.dataStore.data.map { it[SettingsKeys.CALORIES_DAILY_GOAL] ?: 2000 }
    val theme: Flow<ThemeMode> = context.dataStore.data.map {
        when (it[SettingsKeys.THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }
    val language: Flow<String?> = context.dataStore.data.map { it[SettingsKeys.LANGUAGE] }
    val moduleTodo: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.MODULE_TODO] ?: true }
    val moduleCalories: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.MODULE_CALORIES] ?: true }
    val moduleMeditation: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.MODULE_MEDITATION] ?: true }

    val weightTarget: Flow<Float> = context.dataStore.data.map { it[SettingsKeys.WEIGHT_TARGET] ?: 0f }
    val weightWeighInDay: Flow<Int> = context.dataStore.data.map { it[SettingsKeys.WEIGHT_WEIGH_IN_DAY] ?: java.util.Calendar.SUNDAY }
    val moduleWeight: Flow<Boolean> = context.dataStore.data.map { it[SettingsKeys.MODULE_WEIGHT] ?: true }

    val completedTaskColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.COMPLETED_TASK_COLOR] ?: "#9E9E9E" }
    // null означает «цвет по умолчанию из темы» (контрастный к фону)
    val deadlineSafeColor: Flow<String?> = context.dataStore.data.map { it[SettingsKeys.DEADLINE_SAFE_COLOR] }
    val deadlineWarningColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.DEADLINE_WARNING_COLOR] ?: "#FFC107" }
    val deadlineDangerColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.DEADLINE_DANGER_COLOR] ?: "#F44336" }
    val progressBarColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.PROGRESS_BAR_COLOR] ?: "#4CAF50" }
    val approachingGoalColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.APPROACHING_GOAL_COLOR] ?: "#FF9800" }
    val exceedingGoalColor: Flow<String> = context.dataStore.data.map { it[SettingsKeys.EXCEEDING_GOAL_COLOR] ?: "#F44336" }

    suspend fun setCaloriesDailyGoal(goal: Int) {
        context.dataStore.edit { it[SettingsKeys.CALORIES_DAILY_GOAL] = goal }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[SettingsKeys.THEME] = mode.name }
    }

    suspend fun setLanguage(lang: String?) {
        context.dataStore.edit {
            if (lang == null) it.remove(SettingsKeys.LANGUAGE)
            else it[SettingsKeys.LANGUAGE] = lang
        }
    }

    suspend fun setModuleEnabled(module: String, enabled: Boolean) {
        context.dataStore.edit {
            when (module) {
                "todo" -> it[SettingsKeys.MODULE_TODO] = enabled
                "calories" -> it[SettingsKeys.MODULE_CALORIES] = enabled
                "meditation" -> it[SettingsKeys.MODULE_MEDITATION] = enabled
                "weight" -> it[SettingsKeys.MODULE_WEIGHT] = enabled
            }
        }
    }

    suspend fun setWeightTarget(target: Float) {
        context.dataStore.edit { it[SettingsKeys.WEIGHT_TARGET] = target }
    }

    suspend fun setWeightWeighInDay(day: Int) {
        context.dataStore.edit { it[SettingsKeys.WEIGHT_WEIGH_IN_DAY] = day }
    }

    suspend fun setColor(key: Preferences.Key<String>, color: String) {
        context.dataStore.edit { it[key] = color }
    }
}
