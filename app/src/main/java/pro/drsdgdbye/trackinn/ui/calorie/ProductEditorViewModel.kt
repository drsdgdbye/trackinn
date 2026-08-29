package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.ProductRepository

class ProductEditorViewModel(private val repository: ProductRepository) : ViewModel() {

    val name = MutableStateFlow("")
    val category = MutableStateFlow("")
    val unit = MutableStateFlow("GRAM")
    val calories = MutableStateFlow("")
    val protein = MutableStateFlow("")
    val fat = MutableStateFlow("")
    val carbs = MutableStateFlow("")

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    private var productId: Long = -1

    fun loadProduct(id: Long) {
        if (id <= 0 || _isLoaded.value) return
        productId = id
        viewModelScope.launch {
            val product = repository.getById(id) ?: return@launch
            name.value = product.name
            category.value = product.category ?: ""
            unit.value = product.unit
            calories.value = product.caloriesPer100.toString()
            protein.value = product.proteinPer100.toString()
            fat.value = product.fatPer100.toString()
            carbs.value = product.carbsPer100.toString()
            _isLoaded.value = true
        }
    }

    fun saveProduct(onSuccess: () -> Unit) {
        val n = name.value.trim()
        val cal = calories.value.toIntOrNull() ?: return
        if (n.isBlank()) return

        viewModelScope.launch {
            val product = ProductEntity(
                id = if (productId > 0) productId else 0,
                name = n,
                category = category.value.ifBlank { null },
                unit = unit.value,
                caloriesPer100 = cal,
                proteinPer100 = protein.value.toIntOrNull() ?: 0,
                fatPer100 = fat.value.toIntOrNull() ?: 0,
                carbsPer100 = carbs.value.toIntOrNull() ?: 0,
                lastModified = System.currentTimeMillis()
            )
            if (productId > 0) repository.update(product)
            else repository.create(product)
            onSuccess()
        }
    }

    fun deleteProduct(onSuccess: () -> Unit) {
        if (productId <= 0) return
        viewModelScope.launch {
            val product = repository.getById(productId) ?: return@launch
            repository.delete(product)
            onSuccess()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProductEditorViewModel(appContainer().productRepository)
            }
        }
    }
}
