package pro.drsdgdbye.trackinn.ui.calorie

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.drsdgdbye.trackinn.R
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DishListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onDishClick: (dishId: Long) -> Unit,
    viewModel: DishListViewModel = viewModel()
) {
    val dishes by viewModel.dishes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dishes)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("dish.list.back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.testTag("dish.list.add")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_dish))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.search_dish)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.testTag("dish.list.search.clear")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dish.list.search")
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.setSelectedType(null) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .testTag("dish.list.filter.all")
                )
                viewModel.dishTypes.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            viewModel.setSelectedType(if (selectedType == type) null else type)
                        },
                        label = { Text(dishTypeLabel(type)) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("dish.list.filter.${type.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (dishes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.no_dishes), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.no_dishes_hint), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val grouped = dishes.groupBy { it.dishType }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    grouped.forEach { (type, dishesOfType) ->
                        item {
                            Text(
                                text = dishTypeLabel(type),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(dishesOfType, key = { it.id }) { dish ->
                            DishItem(dish = dish, onClick = { onDishClick(dish.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishItem(dish: CompositeDishEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dish.list.item.${dish.id}.row")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dish.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
