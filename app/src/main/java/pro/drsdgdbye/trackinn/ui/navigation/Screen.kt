package pro.drsdgdbye.trackinn.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import pro.drsdgdbye.trackinn.R

sealed class Screen(val route: String) {
    data object Todo : Screen("todo")
    data object TaskDetail : Screen("task_detail?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) = if (taskId != null) "task_detail?taskId=$taskId" else "task_detail"
    }
    data object Calorie : Screen("calorie")
    data object CalorieHistory : Screen("calorie_history")
    data object AddMeal : Screen("add_meal?mealType={mealType}&date={date}") {
        fun createRoute(mealType: String, date: Long) = "add_meal?mealType=$mealType&date=$date"
    }
    data object DishList : Screen("dish_list")
    data object DishEditor : Screen("dish_editor?dishId={dishId}") {
        fun createRoute(dishId: Long? = null) = if (dishId != null) "dish_editor?dishId=$dishId" else "dish_editor"
    }
    data object ProductList : Screen("product_list")
    data object ProductEditor : Screen("product_editor?productId={productId}") {
        fun createRoute(productId: Long? = null) = if (productId != null) "product_editor?productId=$productId" else "product_editor"
    }
    data object Meditation : Screen("meditation")
    data object TimerEditor : Screen("timer_editor?timerId={timerId}") {
        fun createRoute(timerId: Long? = null) = if (timerId != null) "timer_editor?timerId=$timerId" else "timer_editor"
    }
    data object TimerRunning : Screen("timer_running?timerId={timerId}") {
        fun createRoute(timerId: Long) = "timer_running?timerId=$timerId"
    }
    data object MeditationHistory : Screen("meditation_history")
    data object Settings : Screen("settings")
}

data class BottomNavItem(
    val screen: Screen,
    @StringRes val label: Int,
    val icon: ImageVector
)

@Composable
fun bottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem(Screen.Todo, R.string.nav_todo, Icons.Default.CheckCircle),
    BottomNavItem(Screen.Calorie, R.string.nav_calories, Icons.Default.LocalFireDepartment),
    BottomNavItem(Screen.Meditation, R.string.nav_meditation, Icons.Default.SelfImprovement)
)
