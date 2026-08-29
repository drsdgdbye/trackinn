package pro.drsdgdbye.trackinn.ui.calorie

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pro.drsdgdbye.trackinn.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorScreen(
    productId: Long = -1,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDelete: () -> Unit = {},
    viewModel: ProductEditorViewModel = viewModel()
) {
    val isEditMode = productId > 0

    LaunchedEffect(productId) {
        if (isEditMode) viewModel.loadProduct(productId)
    }

    val name by viewModel.name.collectAsState()
    val category by viewModel.category.collectAsState()
    val unit by viewModel.unit.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val protein by viewModel.protein.collectAsState()
    val fat by viewModel.fat.collectAsState()
    val carbs by viewModel.carbs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditMode) R.string.edit_product else R.string.new_product))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("product.editor.back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(
                            onClick = {
                                viewModel.deleteProduct(onDelete)
                            },
                            modifier = Modifier.testTag("product.editor.delete")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text(stringResource(R.string.product_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.name")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.unit_label))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = unit == "GRAM",
                    onClick = { viewModel.unit.value = "GRAM" },
                    modifier = Modifier.testTag("product.editor.unit.grams")
                )
                Text(stringResource(R.string.unit_grams))
                RadioButton(
                    selected = unit == "ML",
                    onClick = { viewModel.unit.value = "ML" },
                    modifier = Modifier.testTag("product.editor.unit.ml")
                )
                Text(stringResource(R.string.unit_ml))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.per_100_label))

            OutlinedTextField(
                value = calories,
                onValueChange = { viewModel.calories.value = it },
                label = { Text(stringResource(R.string.calories_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.calories")
            )
            OutlinedTextField(
                value = protein,
                onValueChange = { viewModel.protein.value = it },
                label = { Text(stringResource(R.string.protein_short_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.protein")
            )
            OutlinedTextField(
                value = fat,
                onValueChange = { viewModel.fat.value = it },
                label = { Text(stringResource(R.string.fat_short_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.fat")
            )
            OutlinedTextField(
                value = carbs,
                onValueChange = { viewModel.carbs.value = it },
                label = { Text(stringResource(R.string.carbs_short_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.carbs")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { viewModel.category.value = it },
                label = { Text(stringResource(R.string.category_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.category")
            )

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { viewModel.saveProduct(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product.editor.save")
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
