package pro.drsdgdbye.trackinn.ui.calorie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.CompositeDishEntity
import pro.drsdgdbye.trackinn.data.db.entity.MealItemEntity
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.CompositeDishRepository
import pro.drsdgdbye.trackinn.data.repository.MealRepository
import pro.drsdgdbye.trackinn.data.repository.NutrientCalculation
import pro.drsdgdbye.trackinn.data.repository.ProductRepository

sealed class SearchResult {
    data class Product(val product: ProductEntity) : SearchResult()
    data class Dish(val dish: CompositeDishEntity) : SearchResult()
}

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddMealViewModel(
    private val productRepository: ProductRepository,
    private val dishRepository: CompositeDishRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val searchResults: StateFlow<List<SearchResult>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else {
                val products = productRepository.search(query)
                val dishes = dishRepository.search(query)
                kotlinx.coroutines.flow.combine(products, dishes) { p, d ->
                    p.map { SearchResult.Product(it) } + d.map { SearchResult.Dish(it) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    suspend fun createProduct(
        name: String,
        category: String?,
        unit: String,
        caloriesPer100: Int,
        proteinPer100: Int,
        fatPer100: Int,
        carbsPer100: Int
    ): SearchResult.Product {
        val id = productRepository.create(
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
        val product = productRepository.getById(id)!!
        return SearchResult.Product(product)
    }

    fun addItem(mealType: String, date: Long, item: SearchResult, weight: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            val mealId = mealRepository.getOrCreateMeal(date, mealType)
            when (item) {
                is SearchResult.Product -> {
                    val caloriesPer100 = item.product.caloriesPer100
                    val calories = (weight * caloriesPer100 / 100.0).toInt()
                    mealRepository.addItem(
                        mealId,
                        MealItemEntity(
                            mealId = mealId,
                            productId = item.product.id,
                            name = item.product.name,
                            weight = weight,
                            calories = calories
                        )
                    )
                }
                is SearchResult.Dish -> {
                    val dish = item.dish
                    val ingredients = dishRepository.getIngredientsList(dish.id)
                    if (ingredients.isNotEmpty() && dish.cookedWeightGrams > 0) {
                        val productIds = ingredients.map { it.productId }.distinct()
                        val productMap = productIds.associateWith { id ->
                            productRepository.getById(id)
                        }
                        val ingredientNutrients = ingredients.mapNotNull { entity ->
                            val product = productMap[entity.productId] ?: return@mapNotNull null
                            NutrientCalculation.IngredientNutrients(
                                quantity = entity.quantity,
                                caloriesPer100 = product.caloriesPer100,
                                proteinPer100 = product.proteinPer100,
                                fatPer100 = product.fatPer100,
                                carbsPer100 = product.carbsPer100
                            )
                        }
                        val totalNutrients = NutrientCalculation.totals(ingredientNutrients)
                        val caloriesPerGram = totalNutrients.calories.toFloat() / dish.cookedWeightGrams
                        val calories = (weight * caloriesPerGram).toInt()
                        mealRepository.addItem(
                            mealId,
                            MealItemEntity(
                                mealId = mealId,
                                compositeDishId = dish.id,
                                name = dish.name,
                                weight = weight,
                                calories = calories
                            )
                        )
                    }
                }
            }
            onDone()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AddMealViewModel(
                    container.productRepository,
                    container.compositeDishRepository,
                    container.mealRepository
                )
            }
        }
    }
}
