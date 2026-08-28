package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.CompositeDishRepository

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DishListViewModel(private val repository: CompositeDishRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<String?>(null)

    val searchQuery: StateFlow<String> = _searchQuery
    val selectedType: StateFlow<String?> = _selectedType

    val dishTypes = listOf("SOUP", "MAIN", "SIDE", "SALAD", "DESSERT", "SNACK", "DRINK", "OTHER")

    val dishes: StateFlow<List<CompositeDishEntity>> = combine(
        _searchQuery.debounce(300).flatMapLatest { query ->
            if (query.isBlank()) repository.getAll()
            else repository.search(query)
        },
        _selectedType
    ) { allDishes, type ->
        if (type == null) allDishes
        else allDishes.filter { it.dishType == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedType(type: String?) {
        _selectedType.value = type
    }

    fun deleteDish(dish: CompositeDishEntity) {
        viewModelScope.launch {
            repository.delete(dish)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                DishListViewModel(appContainer().compositeDishRepository)
            }
        }
    }
}
