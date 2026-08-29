package pro.drsdgdbye.trackinn.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.WeightEntryEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.WeightEntryRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class WeightViewModel(
    private val repository: WeightEntryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val entries: StateFlow<List<WeightEntryEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightTarget: StateFlow<Float> = settingsRepository.weightTarget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val weightWeighInDay: StateFlow<Int> = settingsRepository.weightWeighInDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.util.Calendar.SUNDAY)

    fun addEntry(weightKg: Double) {
        viewModelScope.launch { repository.add(weightKg) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                WeightViewModel(container.weightEntryRepository, container.settingsRepository)
            }
        }
    }
}
