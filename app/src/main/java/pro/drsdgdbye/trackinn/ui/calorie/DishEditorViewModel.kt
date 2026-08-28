package pro.drsdgdbye.trackinn.ui.calorie

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishIngredientEntity
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.repository.CompositeDishRepository
import pro.drsdgdbye.trackinn.data.repository.NutrientCalculation
import pro.drsdgdbye.trackinn.data.repository.Nutrients
import pro.drsdgdbye.trackinn.data.repository.ProductRepository

data class IngredientRow(
    val id: Long = 0,
    val productId: Long = 0,
    val productName: String = "",
    val quantity: Int = 0,
    val position: Int = 0
)

private data class DishSnapshot(
    val name: String,
    val type: String,
    val cookedWeight: String,
    val ingredients: List<IngredientRow>
)

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DishEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val dishRepository: CompositeDishRepository
    private val productRepository: ProductRepository

    val dishName = MutableStateFlow("")
    val dishType = MutableStateFlow("MAIN")
    val cookedWeight = MutableStateFlow("")
    val ingredients = MutableStateFlow<List<IngredientRow>>(emptyList())
    val isLoaded = MutableStateFlow(false)
    val dishNameError = MutableStateFlow(false)

    private val productSearchQuery = MutableStateFlow("")

    val productSearchResults: StateFlow<List<ProductEntity>> = productSearchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else productRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts = MutableStateFlow<List<ProductEntity>>(emptyList())

    private var initialSnapshot: DishSnapshot? = null

    fun markInitialState() {
        initialSnapshot = currentSnapshot()
    }

    fun hasUnsavedChanges(): Boolean {
        val initial = initialSnapshot ?: return true
        return initial != currentSnapshot()
    }

    fun markSaved() {
        initialSnapshot = currentSnapshot()
    }

    private fun currentSnapshot() = DishSnapshot(
        name = dishName.value,
        type = dishType.value,
        cookedWeight = cookedWeight.value,
        ingredients = ingredients.value
    )

    // КБЖУ блюда на 100 г готового блюда — пересчитывается реактивно при
    // изменении веса, ингредиентов или каталога продуктов (включая новые продукты)
    val dishNutrients: StateFlow<Nutrients> = combine(
        cookedWeight,
        ingredients,
        allProducts
    ) { weightText, rows, products ->
        val weight = weightText.toIntOrNull() ?: 0
        val productMap = products.associateBy { it.id }
        val ingredientNutrients = rows.mapNotNull { row ->
            val product = productMap[row.productId] ?: return@mapNotNull null
            if (row.quantity <= 0) return@mapNotNull null
            NutrientCalculation.IngredientNutrients(
                quantity = row.quantity,
                caloriesPer100 = product.caloriesPer100,
                proteinPer100 = product.proteinPer100,
                fatPer100 = product.fatPer100,
                carbsPer100 = product.carbsPer100
            )
        }
        NutrientCalculation.per100(ingredientNutrients, weight)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Nutrients(0, 0, 0, 0))

    init {
        val db = TrackinnDatabase.getInstance(application)
        dishRepository = CompositeDishRepository(db.compositeDishDao())
        productRepository = ProductRepository(db.productDao())
        loadAllProducts()
    }

    private fun loadAllProducts() {
        viewModelScope.launch {
            productRepository.getAll().collect { allProducts.value = it }
        }
    }

    fun setProductSearchQuery(query: String) {
        productSearchQuery.value = query
    }

    fun loadDish(dishId: Long) {
        if (dishId <= 0) {
            isLoaded.value = true
            markInitialState()
            return
        }
        viewModelScope.launch {
            val dish = dishRepository.getById(dishId) ?: return@launch
            val ingredientEntities = dishRepository.getIngredientsList(dishId)
            val productIds = ingredientEntities.map { it.productId }.distinct()
            val productMap = productIds.associateWith { id ->
                productRepository.getById(id)
            }
            dishName.value = dish.name
            dishType.value = dish.dishType
            cookedWeight.value = dish.cookedWeightGrams.toString()
            ingredients.value = ingredientEntities.map { entity ->
                IngredientRow(
                    id = entity.id,
                    productId = entity.productId,
                    productName = productMap[entity.productId]?.name ?: "Неизвестный",
                    quantity = entity.quantity,
                    position = entity.position
                )
            }
            isLoaded.value = true
            markInitialState()
        }
    }

    fun addIngredient(product: ProductEntity) {
        val current = ingredients.value.toMutableList()
        val position = current.size
        current.add(
            IngredientRow(
                productId = product.id,
                productName = product.name,
                quantity = 100,
                position = position
            )
        )
        ingredients.value = current
        productSearchQuery.value = ""
    }

    fun createProductAndAdd(name: String, calories: Int, protein: Int, fat: Int, carbs: Int) {
        viewModelScope.launch {
            val id = productRepository.create(
                ProductEntity(
                    name = name,
                    unit = "GRAM",
                    caloriesPer100 = calories,
                    proteinPer100 = protein,
                    fatPer100 = fat,
                    carbsPer100 = carbs
                )
            )
            addIngredient(
                ProductEntity(
                    id = id,
                    name = name,
                    unit = "GRAM",
                    caloriesPer100 = calories,
                    proteinPer100 = protein,
                    fatPer100 = fat,
                    carbsPer100 = carbs
                )
            )
        }
    }

    fun updateIngredientQuantity(index: Int, quantity: Int) {
        val current = ingredients.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(quantity = quantity)
            ingredients.value = current
        }
    }

    fun removeIngredient(index: Int) {
        val current = ingredients.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            current.forEachIndexed { i, row ->
                current[i] = row.copy(position = i)
            }
            ingredients.value = current
        }
    }

    fun saveDish(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val name = dishName.value.trim()
            if (name.isBlank()) return@launch
            val weight = cookedWeight.value.toIntOrNull() ?: return@launch
            if (weight <= 0) return@launch
            if (ingredients.value.isEmpty()) return@launch

            val existingByName = dishRepository.getByName(name)
            if (existingByName != null) {
                dishNameError.value = true
                return@launch
            }
            val dish = CompositeDishEntity(
                id = 0,
                name = name,
                dishType = dishType.value,
                cookedWeightGrams = weight
            )
            val ingredientEntities = ingredients.value.mapIndexed { index, row ->
                CompositeDishIngredientEntity(
                    id = row.id,
                    dishId = 0,
                    productId = row.productId,
                    quantity = row.quantity,
                    position = index
                )
            }
            val dishId = dishRepository.create(dish, ingredientEntities)
            markSaved()
            onSuccess(dishId)
        }
    }

    fun updateDish(dishId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val name = dishName.value.trim()
            if (name.isBlank()) return@launch
            val weight = cookedWeight.value.toIntOrNull() ?: return@launch
            if (weight <= 0) return@launch

            val existingByName = dishRepository.getByName(name)
            if (existingByName != null && existingByName.id != dishId) {
                dishNameError.value = true
                return@launch
            }

            val dish = CompositeDishEntity(
                id = dishId,
                name = name,
                dishType = dishType.value,
                cookedWeightGrams = weight
            )
            val ingredientEntities = ingredients.value.mapIndexed { index, row ->
                CompositeDishIngredientEntity(
                    id = row.id,
                    dishId = dishId,
                    productId = row.productId,
                    quantity = row.quantity,
                    position = index
                )
            }
            dishRepository.update(dish, ingredientEntities)
            markSaved()
            onSuccess()
        }
    }

    fun deleteDish(dishId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val dish = dishRepository.getById(dishId) ?: return@launch
            dishRepository.delete(dish)
            onSuccess()
        }
    }
}
