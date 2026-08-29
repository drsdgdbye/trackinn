package pro.drsdgdbye.trackinn

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pro.drsdgdbye.trackinn.ui.calorie.AddMealScreen
import pro.drsdgdbye.trackinn.ui.calorie.AddMealViewModel
import pro.drsdgdbye.trackinn.ui.calorie.CalorieHistoryScreen
import pro.drsdgdbye.trackinn.ui.calorie.CalorieHistoryViewModel
import pro.drsdgdbye.trackinn.ui.calorie.CalorieScreen
import pro.drsdgdbye.trackinn.ui.calorie.CalorieViewModel
import pro.drsdgdbye.trackinn.ui.calorie.DishEditorScreen
import pro.drsdgdbye.trackinn.ui.calorie.DishEditorViewModel
import pro.drsdgdbye.trackinn.ui.calorie.DishListScreen
import pro.drsdgdbye.trackinn.ui.calorie.DishListViewModel
import pro.drsdgdbye.trackinn.ui.calorie.ProductEditorScreen
import pro.drsdgdbye.trackinn.ui.calorie.ProductEditorViewModel
import pro.drsdgdbye.trackinn.ui.calorie.ProductListScreen
import pro.drsdgdbye.trackinn.ui.calorie.ProductListViewModel
import pro.drsdgdbye.trackinn.ui.meditation.MeditationScreen
import pro.drsdgdbye.trackinn.ui.meditation.MeditationViewModel
import pro.drsdgdbye.trackinn.ui.meditation.MeditationHistoryScreen
import pro.drsdgdbye.trackinn.ui.meditation.TimerEditorScreen
import pro.drsdgdbye.trackinn.ui.meditation.TimerRunningScreen
import pro.drsdgdbye.trackinn.ui.navigation.Screen
import pro.drsdgdbye.trackinn.ui.navigation.bottomNavItems
import pro.drsdgdbye.trackinn.ui.settings.SettingsScreen
import pro.drsdgdbye.trackinn.ui.todo.TaskDetailScreen
import pro.drsdgdbye.trackinn.ui.todo.TodoScreen
import pro.drsdgdbye.trackinn.ui.todo.TodoViewModel
import pro.drsdgdbye.trackinn.ui.weight.WeightScreen
import pro.drsdgdbye.trackinn.ui.weight.WeightViewModel
import pro.drsdgdbye.trackinn.ui.weight.WeightHistoryScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import pro.drsdgdbye.trackinn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackinnRoot() {
    val context = LocalContext.current
    val container = (context.applicationContext as TrackinnApp).container
    val settingsRepository = container.settingsRepository
    val moduleTodo by settingsRepository.moduleTodo.collectAsState(initial = true)
    val moduleCalories by settingsRepository.moduleCalories.collectAsState(initial = true)
    val moduleMeditation by settingsRepository.moduleMeditation.collectAsState(initial = true)
    val moduleWeight by settingsRepository.moduleWeight.collectAsState(initial = true)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val enabledItems = bottomNavItems().filter { item ->
        when (item.screen) {
            Screen.Todo -> moduleTodo
            Screen.Calorie -> moduleCalories
            Screen.Meditation -> moduleMeditation
            Screen.Weight -> moduleWeight
            else -> true
        }
    }

    val startDestination = when {
        moduleTodo -> Screen.Todo.route
        moduleCalories -> Screen.Calorie.route
        moduleMeditation -> Screen.Meditation.route
        moduleWeight -> Screen.Weight.route
        else -> Screen.Settings.route
    }

    val mainRoutes = listOf(Screen.Todo.route, Screen.Calorie.route, Screen.Meditation.route, Screen.Weight.route)
    val showTopBar = currentRoute in mainRoutes

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_title)) },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate(Screen.Settings.route) },
                            modifier = Modifier.testTag("nav.settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (enabledItems.isNotEmpty()) {
                NavigationBar {
                    enabledItems.forEach { item ->
                        val tabTag = when (item.screen) {
                            Screen.Todo -> "nav.tab.todo"
                            Screen.Calorie -> "nav.tab.calorie"
                            Screen.Meditation -> "nav.tab.meditation"
                            Screen.Weight -> "nav.tab.weight"
                            else -> null
                        }
                        NavigationBarItem(
                            modifier = tabTag?.let { Modifier.testTag(it) } ?: Modifier,
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Todo.route) {
                val todoViewModel: TodoViewModel = viewModel(factory = TodoViewModel.Factory)
                TodoScreen(
                    onAddClick = { navController.navigate(Screen.TaskDetail.createRoute()) },
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.createRoute(taskId)) },
                    viewModel = todoViewModel
                )
            }
            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                val todoViewModel: TodoViewModel = viewModel(factory = TodoViewModel.Factory)
                TaskDetailScreen(
                    taskId = taskId,
                    onBack = { navController.popBackStack() },
                    viewModel = todoViewModel
                )
            }
            composable(Screen.Calorie.route) {
                val calorieViewModel: CalorieViewModel = viewModel(factory = CalorieViewModel.Factory)
                CalorieScreen(
                    onAddMealClick = { mealType, date ->
                        navController.navigate(Screen.AddMeal.createRoute(mealType, date))
                    },
                    onDishListClick = {
                        navController.navigate(Screen.DishList.route)
                    },
                    onProductListClick = {
                        navController.navigate(Screen.ProductList.route)
                    },
                    onHistoryClick = {
                        navController.navigate(Screen.CalorieHistory.route)
                    },
                    viewModel = calorieViewModel
                )
            }
            composable(Screen.CalorieHistory.route) {
                val calorieHistoryViewModel: CalorieHistoryViewModel = viewModel(factory = CalorieHistoryViewModel.Factory)
                CalorieHistoryScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = calorieHistoryViewModel
                )
            }
            composable(
                route = Screen.AddMeal.route,
                arguments = listOf(
                    navArgument("mealType") { type = NavType.StringType },
                    navArgument("date") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val mealType = backStackEntry.arguments?.getString("mealType") ?: "BREAKFAST"
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                val addMealViewModel: AddMealViewModel = viewModel(factory = AddMealViewModel.Factory)
                AddMealScreen(
                    mealType = mealType,
                    date = date,
                    onBack = { navController.popBackStack() },
                    onItemAdded = { navController.popBackStack() },
                    viewModel = addMealViewModel
                )
            }
            composable(Screen.DishList.route) {
                val dishListViewModel: DishListViewModel = viewModel(factory = DishListViewModel.Factory)
                DishListScreen(
                    onBack = { navController.popBackStack() },
                    onAddClick = { navController.navigate(Screen.DishEditor.createRoute()) },
                    onDishClick = { dishId -> navController.navigate(Screen.DishEditor.createRoute(dishId)) },
                    viewModel = dishListViewModel
                )
            }
            composable(
                route = Screen.DishEditor.route,
                arguments = listOf(navArgument("dishId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val dishId = backStackEntry.arguments?.getLong("dishId") ?: -1L
                val dishEditorViewModel: DishEditorViewModel = viewModel(factory = DishEditorViewModel.Factory)
                DishEditorScreen(
                    dishId = dishId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    viewModel = dishEditorViewModel
                )
            }
            composable(Screen.ProductList.route) {
                val productListViewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory)
                ProductListScreen(
                    onBack = { navController.popBackStack() },
                    onProductClick = { productId -> navController.navigate(Screen.ProductEditor.createRoute(productId)) },
                    onAddClick = { navController.navigate(Screen.ProductEditor.createRoute()) },
                    viewModel = productListViewModel
                )
            }
            composable(
                route = Screen.ProductEditor.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: -1L
                val productEditorViewModel: ProductEditorViewModel = viewModel(factory = ProductEditorViewModel.Factory)
                ProductEditorScreen(
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onDelete = { navController.popBackStack() },
                    viewModel = productEditorViewModel
                )
            }
            composable(Screen.Meditation.route) { backStackEntry ->
                val meditationBackStackEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.Meditation.route) }
                val meditationViewModel: MeditationViewModel = viewModel(meditationBackStackEntry, factory = MeditationViewModel.Factory)
                MeditationScreen(
                    onAddClick = { navController.navigate(Screen.TimerEditor.createRoute()) },
                    onTimerClick = { timerId -> navController.navigate(Screen.TimerEditor.createRoute(timerId)) },
                    onStartTimer = { timerId -> navController.navigate(Screen.TimerRunning.createRoute(timerId)) },
                    onHistoryClick = { navController.navigate(Screen.MeditationHistory.route) },
                    viewModel = meditationViewModel
                )
            }
            composable(
                route = Screen.TimerEditor.route,
                arguments = listOf(navArgument("timerId") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getLong("timerId") ?: -1L
                val meditationBackStackEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.Meditation.route) }
                val meditationViewModel: MeditationViewModel = viewModel(meditationBackStackEntry, factory = MeditationViewModel.Factory)
                TimerEditorScreen(
                    timerId = timerId,
                    onBack = { navController.popBackStack() },
                    viewModel = meditationViewModel
                )
            }
            composable(
                route = Screen.TimerRunning.route,
                arguments = listOf(navArgument("timerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val timerId = backStackEntry.arguments?.getLong("timerId") ?: -1L
                val meditationBackStackEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.Meditation.route) }
                val meditationViewModel: MeditationViewModel = viewModel(meditationBackStackEntry, factory = MeditationViewModel.Factory)
                TimerRunningScreen(
                    timerId = timerId,
                    onBack = { navController.popBackStack() },
                    viewModel = meditationViewModel
                )
            }
            composable(Screen.MeditationHistory.route) { backStackEntry ->
                val meditationBackStackEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.Meditation.route) }
                val meditationViewModel: MeditationViewModel = viewModel(meditationBackStackEntry, factory = MeditationViewModel.Factory)
                MeditationHistoryScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = meditationViewModel
                )
            }
            composable(Screen.Weight.route) {
                val weightViewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
                WeightScreen(
                    onHistoryClick = { navController.navigate(Screen.WeightHistory.route) },
                    viewModel = weightViewModel
                )
            }
            composable(Screen.WeightHistory.route) {
                val weightViewModel: WeightViewModel = viewModel(factory = WeightViewModel.Factory)
                WeightHistoryScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = weightViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
