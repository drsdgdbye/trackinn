package pro.drsdgdbye.trackinn.ui.calorie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    viewModel: CalorieViewModel = viewModel()
) {
    val meals by viewModel.meals.collectAsState()
    val today = remember { System.currentTimeMillis() }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val caloriesDailyGoal by viewModel.caloriesDailyGoal.collectAsState(initial = 2000)
    val totalCalories = meals.sumOf { meal -> meal.items.sumOf { it.calories } }
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
        floatingActionButton = {
            FloatingActionButton(onClick = onDishListClick) {
                Icon(Icons.Default.Restaurant, contentDescription = stringResource(R.string.dishes))
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
            IconButton(onClick = onAddClick) {
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
                modifier = Modifier.width(96.dp)
            )
            Text(
                stringResource(R.string.table_kcal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.width(80.dp)
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
                        modifier = Modifier.width(96.dp)
                    )
                    Text(
                        stringResource(R.string.calories_short, item.calories),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(80.dp)
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
                    modifier = Modifier.fillMaxWidth()
                )
                if (weight != null && weight > 0) {
                    val newCalories = if (item.weight > 0) {
                        (item.calories.toLong() * weight / item.weight).toInt()
                    } else item.calories
                    Text(
                        stringResource(R.string.approx_calories, newCalories),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    weight?.takeIf { it > 0 }?.let(onSave)
                },
                enabled = weight != null && weight > 0
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
