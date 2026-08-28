package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.ProductRepository

@OptIn(FlowPreview::class)
class ProductSearchViewModel(private val repository: ProductRepository) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val searchResults = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else repository.search(query)
        }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun createProduct(
        name: String,
        category: String?,
        unit: String,
        caloriesPer100: Int,
        proteinPer100: Int,
        fatPer100: Int,
        carbsPer100: Int
    ): Long {
        var id = 0L
        viewModelScope.launch {
            id = repository.create(
                ProductEntity(
                    name = name,
                    category = category,
                    unit = unit,
                    caloriesPer100 = caloriesPer100,
                    proteinPer100 = proteinPer100,
                    fatPer100 = fatPer100,
                    carbsPer100 = carbsPer100
                )
            )
        }
        return id
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ProductSearchViewModel(appContainer().productRepository)
            }
        }
    }
}
