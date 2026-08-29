package pro.drsdgdbye.trackinn.ui.calorie

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.R

@Composable
private fun mealTypeLabel(type: String): String = when (type) {
    "BREAKFAST" -> stringResource(R.string.meal_breakfast)
    "LUNCH" -> stringResource(R.string.meal_lunch)
    "SNACK" -> stringResource(R.string.meal_snack)
    "DINNER" -> stringResource(R.string.meal_dinner)
    else -> type
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    mealType: String,
    date: Long,
    onBack: () -> Unit,
    onItemAdded: () -> Unit,
    viewModel: AddMealViewModel = viewModel(),
    productSearchViewModel: ProductSearchViewModel = viewModel(factory = ProductSearchViewModel.Factory)
) {
    val searchResults by viewModel.searchResults.collectAsState()
    var weight by remember { mutableStateOf("") }
    var selectedResult by remember { mutableStateOf<SearchResult?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_to_meal, mealTypeLabel(mealType))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val w = weight.toIntOrNull() ?: return@FloatingActionButton
                    val item = selectedResult ?: return@FloatingActionButton
                    viewModel.addItem(mealType, date, item, w) { onItemAdded() }
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.add))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.setSearchQuery(it)
                },
                placeholder = { Text(stringResource(R.string.search_product)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedResult != null) {
                val item = selectedResult!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when (item) {
                                is SearchResult.Product -> item.product.name
                                is SearchResult.Dish -> item.dish.name
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            when (item) {
                                is SearchResult.Product -> stringResource(R.string.calories_per_100g, item.product.caloriesPer100)
                                is SearchResult.Dish -> stringResource(R.string.composite_dish)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(stringResource(R.string.weight_gram)) },
                    modifier = Modifier.fillMaxWidth()
                )

            }

            Spacer(modifier = Modifier.height(12.dp))

            if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.create_new_product))
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(searchResults) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedResult = result
                                weight = when (result) {
                                    is SearchResult.Product -> "100"
                                    is SearchResult.Dish -> result.dish.cookedWeightGrams.toString()
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when (result) {
                                    is SearchResult.Product -> result.product.name
                                    is SearchResult.Dish -> result.dish.name
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                when (result) {
                                    is SearchResult.Product -> stringResource(R.string.calories_per_100g, result.product.caloriesPer100)
                                    is SearchResult.Dish -> "${result.dish.cookedWeightGrams}${stringResource(R.string.gram_short)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProductDialog(
            initialName = searchQuery,
            onDismiss = { showCreateDialog = false },
            onCreated = { product ->
                showCreateDialog = false
                selectedResult = product
                weight = "100"
                searchQuery = product.product.name
                viewModel.setSearchQuery(product.product.name)
            },
            viewModel = viewModel
        )
    }
}

@Composable
private fun CreateProductDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onCreated: (SearchResult.Product) -> Unit,
    viewModel: AddMealViewModel
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_product)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.product_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    val c = calories.toIntOrNull() ?: 0
                    val p = protein.toIntOrNull() ?: 0
                    val f = fat.toIntOrNull() ?: 0
                    val cb = carbs.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        scope.launch {
                            val product = viewModel.createProduct(
                                name = name.trim(),
                                category = null,
                                unit = "GRAM",
                                caloriesPer100 = c,
                                proteinPer100 = p,
                                fatPer100 = f,
                                carbsPer100 = cb
                            )
                            onCreated(product)
                        }
                    }
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
