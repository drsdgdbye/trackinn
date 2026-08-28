package pro.drsdgdbye.trackinn.ui.calorie

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R

@Composable
private fun dishTypeLabel(type: String): String = when (type) {
    "SOUP" -> stringResource(R.string.dish_type_soup)
    "MAIN" -> stringResource(R.string.dish_type_main)
    "SIDE" -> stringResource(R.string.dish_type_side)
    "SALAD" -> stringResource(R.string.dish_type_salad)
    "DESSERT" -> stringResource(R.string.dish_type_dessert)
    "SNACK" -> stringResource(R.string.dish_type_snack)
    "DRINK" -> stringResource(R.string.dish_type_drink)
    "OTHER" -> stringResource(R.string.dish_type_other)
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishEditorScreen(
    dishId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: DishEditorViewModel = viewModel()
) {
    val isLoaded by viewModel.isLoaded.collectAsState()
    val dishName by viewModel.dishName.collectAsState()
    val dishType by viewModel.dishType.collectAsState()
    val cookedWeight by viewModel.cookedWeight.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val productSearchResults by viewModel.productSearchResults.collectAsState()
    val dishNutrients by viewModel.dishNutrients.collectAsState()
    val dishNameError by viewModel.dishNameError.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val errorDishNameExists = stringResource(R.string.error_dish_name_exists)

    LaunchedEffect(dishId) {
        viewModel.loadDish(dishId)
    }

    LaunchedEffect(dishNameError) {
        if (dishNameError) {
            snackbarHostState.showSnackbar(errorDishNameExists)
            viewModel.dishNameError.value = false
        }
    }

    if (!isLoaded) {
        return
    }

    fun saveAndExit() {
        showExitDialog = false
        if (dishId > 0) {
            viewModel.updateDish(dishId) { onSaved() }
        } else {
            viewModel.saveDish { onSaved() }
        }
    }

    fun requestExit() {
        // Проверяем изменения в момент выхода — значения берутся из ViewModel напрямую
        if (viewModel.hasUnsavedChanges()) showExitDialog = true else onBack()
    }

    BackHandler {
        requestExit()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (dishId > 0) stringResource(R.string.edit_dish) else stringResource(R.string.new_dish)) },
                navigationIcon = {
                    IconButton(onClick = { requestExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (dishId > 0) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = dishName,
                onValueChange = { viewModel.dishName.value = it },
                label = { Text(stringResource(R.string.dish_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it }
            ) {
                OutlinedTextField(
                    value = dishTypeLabel(dishType),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.dish_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    listOf("SOUP", "MAIN", "SIDE", "SALAD", "DESSERT", "SNACK", "DRINK", "OTHER").forEach { key ->
                        DropdownMenuItem(
                            text = { Text(dishTypeLabel(key)) },
                            onClick = {
                                viewModel.dishType.value = key
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cookedWeight,
                onValueChange = { viewModel.cookedWeight.value = it },
                label = { Text(stringResource(R.string.dish_weight)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.ingredients), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = productSearchQuery,
                onValueChange = {
                    productSearchQuery = it
                    viewModel.setProductSearchQuery(it)
                },
                placeholder = { Text(stringResource(R.string.search_product_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (productSearchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    items(productSearchResults.take(5)) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addIngredient(product)
                                }
                                .padding(8.dp)
                        ) {
                            Text(product.name, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.calories_per_100g, product.caloriesPer100),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (productSearchQuery.isNotBlank() && productSearchResults.isEmpty()) {
                var showCreateDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.create_product_format, productSearchQuery))
                }
                if (showCreateDialog) {
                    CreateProductDialog(
                        name = productSearchQuery,
                        onDismiss = { showCreateDialog = false },
                        onConfirm = { cal, pro, fat, carbs ->
                            viewModel.createProductAndAdd(productSearchQuery, cal, pro, fat, carbs)
                            showCreateDialog = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(ingredients) { index, row ->
                    IngredientRowItem(
                        row = row,
                        onQuantityChange = { qty ->
                            viewModel.updateIngredientQuantity(index, qty)
                        },
                        onRemove = { viewModel.removeIngredient(index) }
                    )
                }
            }

            if (ingredients.isNotEmpty() && cookedWeight.toIntOrNull() != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(stringResource(R.string.calories_short, dishNutrients.calories), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.protein_short, dishNutrients.protein), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.fat_short, dishNutrients.fat), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.carbs_short, dishNutrients.carbs), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_dish)) },
            text = { Text(stringResource(R.string.delete_dish_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDish(dishId) { onSaved() }
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.save_changes_title)) },
            text = { Text(stringResource(R.string.save_changes_message)) },
            confirmButton = {
                TextButton(onClick = { saveAndExit() }) {
                    Text(stringResource(R.string.save_and_exit))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onBack() }) {
                        Text(stringResource(R.string.discard_changes))
                    }
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun IngredientRowItem(
    row: IngredientRow,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            row.productName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = row.quantity.toString(),
            onValueChange = { onQuantityChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.width(80.dp),
            label = { Text(stringResource(R.string.gram_short)) }
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete))
        }
    }
}

@Composable
private fun CreateProductDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: (calories: Int, protein: Int, fat: Int, carbs: Int) -> Unit
) {
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_product)) },
        text = {
            Column {
                Text(" \"$name\"", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text(stringResource(R.string.calories_per_100g_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.protein_per_100g)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text(stringResource(R.string.fat_per_100g)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(stringResource(R.string.carbs_per_100g)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        calories.toIntOrNull() ?: 0,
                        protein.toIntOrNull() ?: 0,
                        fat.toIntOrNull() ?: 0,
                        carbs.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
