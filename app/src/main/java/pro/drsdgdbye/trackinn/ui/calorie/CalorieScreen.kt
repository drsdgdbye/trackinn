package pro.drsdgdbye.trackinn.ui.calorie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.Color as ComposeColor
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieScreen(
    onAddMealClick: (mealType: String, date: Long) -> Unit = { _, _ -> },
    onDishListClick: () -> Unit = {},
    onProductListClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    viewModel: CalorieViewModel = viewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.refreshToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val meals by viewModel.meals.collectAsState()
    val today = remember { System.currentTimeMillis() }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val caloriesDailyGoal by viewModel.caloriesDailyGoal.collectAsState(initial = 2000)
    val totalCalories = meals.sumOf { meal -> meal.items.sumOf { it.calories } }
    val totalProtein = meals.sumOf { meal -> meal.items.sumOf { it.protein } }
    val totalFat = meals.sumOf { meal -> meal.items.sumOf { it.fat } }
    val totalCarbs = meals.sumOf { meal -> meal.items.sumOf { it.carbs } }
    val progress = if (caloriesDailyGoal > 0) totalCalories.toFloat() / caloriesDailyGoal else 0f

    val progressBarColorHex by viewModel.progressBarColor.collectAsState(initial = "#4CAF50")
    val approachingGoalColorHex by viewModel.approachingGoalColor.collectAsState(initial = "#FF9800")
    val exceedingGoalColorHex by viewModel.exceedingGoalColor.collectAsState(initial = "#F44336")

    val progressBarColor = remember(progressBarColorHex) {
        try { ComposeColor(android.graphics.Color.parseColor(progressBarColorHex)) } catch (e: Exception) { ComposeColor(0xFF4CAF50) }
    }
    val approachingGoalColor = remember(approachingGoalColorHex) {
        try { ComposeColor(android.graphics.Color.parseColor(approachingGoalColorHex)) } catch (e: Exception) { ComposeColor(0xFFFF9800) }
    }
    val exceedingGoalColor = remember(exceedingGoalColorHex) {
        try { ComposeColor(android.graphics.Color.parseColor(exceedingGoalColorHex)) } catch (e: Exception) { ComposeColor(0xFFF44336) }
    }

    var editingItem by remember { mutableStateOf<MealItemEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calories_label)) },
                actions = {
                    IconButton(
                        onClick = onHistoryClick,
                        modifier = Modifier.testTag("calorie.history")
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.calorie_history))
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = onProductListClick,
                    modifier = Modifier.testTag("calorie.products")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = stringResource(R.string.product_list))
                }
                FloatingActionButton(
                    onClick = onDishListClick,
                    modifier = Modifier.testTag("calorie.dishes")
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = stringResource(R.string.dishes))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val mealTypes = listOf(
                stringResource(R.string.meal_breakfast) to "BREAKFAST",
                stringResource(R.string.meal_lunch) to "LUNCH",
                stringResource(R.string.meal_snack) to "SNACK",
                stringResource(R.string.meal_dinner) to "DINNER"
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(mealTypes) { (label, type) ->
                    val meal = meals.find { it.meal.type == type }
                    MealSection(
                        label = label,
                        mealType = type,
                        meal = meal,
                        onAddClick = { onAddMealClick(type, today) },
                        onItemClick = { editingItem = it }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp)
            ) {
                Text(stringResource(R.string.calories_format, totalCalories, caloriesDailyGoal))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        progress > 1f -> exceedingGoalColor
                        progress > 0.8f -> approachingGoalColor
                        else -> progressBarColor
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                val totalMacros = totalProtein + totalFat + totalCarbs
                if (totalMacros > 0) {
                    val proteinPct = totalProtein * 100 / totalMacros
                    val fatPct = totalFat * 100 / totalMacros
                    val carbsPct = totalCarbs * 100 / totalMacros
                    Text(
                        stringResource(R.string.macros_percent_format, proteinPct, fatPct, carbsPct),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.macros_format, totalProtein, totalFat, totalCarbs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                MacroRatioBar(
                    protein = totalProtein,
                    fat = totalFat,
                    carbs = totalCarbs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
        }
    }

    editingItem?.let { item ->
        EditMealItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { newWeight ->
                viewModel.updateItem(item, newWeight)
                editingItem = null
            },
            onDelete = {
                viewModel.deleteItem(item.id)
                editingItem = null
            }
        )
    }
}

@Composable
private fun MealSection(
    label: String,
    mealType: String,
    meal: pro.drsdgdbye.trackinn.data.db.dao.MealWithItems?,
    onAddClick: () -> Unit,
    onItemClick: (MealItemEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("calorie.meal.${mealType.lowercase()}.add")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = 2.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.table_name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.table_weight),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(72.dp)
            )
            Text(
                stringResource(R.string.table_protein),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
            Text(
                stringResource(R.string.table_fat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
            Text(
                stringResource(R.string.table_carbs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(40.dp)
            )
            Text(
                stringResource(R.string.table_kcal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(64.dp)
            )
        }

        if (meal == null || meal.items.isEmpty()) {
            Text(
                stringResource(R.string.no_entries),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            meal.items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calorie.item.${item.id}.row")
                        .clickable { onItemClick(item) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${item.weight} ${stringResource(R.string.gram_short)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        item.protein.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        item.fat.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        item.carbs.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(
                        stringResource(R.string.calories_short, item.calories),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(64.dp)
                    )
                }
                if (index < meal.items.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun EditMealItemDialog(
    item: MealItemEntity,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onDelete: () -> Unit
) {
    var weightText by remember(item.id) { mutableStateOf(item.weight.toString()) }
    val weight = weightText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.weight_gram)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calorie.item.edit.weight")
                )
                if (weight != null && weight > 0) {
                    val newCalories = if (item.weight > 0) {
                        (item.calories.toLong() * weight / item.weight).toInt()
                    } else item.calories
                    val newProtein = (item.proteinPer100.toLong() * weight / 100).toInt()
                    val newFat = (item.fatPer100.toLong() * weight / 100).toInt()
                    val newCarbs = (item.carbsPer100.toLong() * weight / 100).toInt()
                    Text(
                        stringResource(R.string.approx_calories, newCalories),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        stringResource(R.string.macros_format, newProtein, newFat, newCarbs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    weight?.takeIf { it > 0 }?.let(onSave)
                },
                enabled = weight != null && weight > 0,
                modifier = Modifier.testTag("calorie.item.edit.save")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("calorie.item.edit.delete")
                ) {
                    Text(stringResource(R.string.delete))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("calorie.item.edit.cancel")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
private fun MacroRatioBar(
    protein: Int,
    fat: Int,
    carbs: Int,
    modifier: Modifier = Modifier
) {
    val total = protein + fat + carbs
    if (total <= 0) {
        Box(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(4.dp)
                )
        )
        return
    }
    val proteinColor = Color(0xFF2196F3)
    val fatColor = Color(0xFFF44336)
    val carbsColor = Color(0xFFFFEB3B)
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .weight(protein.toFloat())
                .fillMaxHeight()
                .background(proteinColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
        )
        Box(
            modifier = Modifier
                .weight(fat.toFloat())
                .fillMaxHeight()
                .background(fatColor)
        )
        Box(
            modifier = Modifier
                .weight(carbs.toFloat())
                .fillMaxHeight()
                .background(carbsColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
        )
    }
}
