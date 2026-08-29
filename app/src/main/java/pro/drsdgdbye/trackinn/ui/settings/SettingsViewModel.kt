package pro.drsdgdbye.trackinn.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.di.AppContainer
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class SettingsViewModel(
    private val container: AppContainer,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _pendingDisableModule = MutableStateFlow<String?>(null)
    val pendingDisableModule: StateFlow<String?> = _pendingDisableModule.asStateFlow()

    private val _showDisableConfirmDialog = MutableStateFlow(false)
    val showDisableConfirmDialog: StateFlow<Boolean> = _showDisableConfirmDialog.asStateFlow()

    private val _wipeFailed = MutableStateFlow(false)
    val wipeFailed: StateFlow<Boolean> = _wipeFailed.asStateFlow()

    fun requestDisable(module: String) {
        _pendingDisableModule.value = module
        _showDisableConfirmDialog.value = true
    }

    fun cancelDisable() {
        _pendingDisableModule.value = null
        _showDisableConfirmDialog.value = false
    }

    fun confirmDisable() {
        val module = _pendingDisableModule.value ?: return
        viewModelScope.launch {
            try {
                when (module) {
                    "todo" -> container.taskRepository.deleteCompleted()
                    "calories" -> container.mealRepository.deleteAll()
                    "meditation" -> container.savedTimerRepository.deleteAllSessions()
                    "weight" -> container.weightEntryRepository.deleteAll()
                }
                settingsRepository.setModuleEnabled(module, false)
                _pendingDisableModule.value = null
                _showDisableConfirmDialog.value = false
            } catch (e: Exception) {
                _wipeFailed.value = true
            }
        }
    }

    fun enable(module: String) {
        viewModelScope.launch { settingsRepository.setModuleEnabled(module, true) }
    }

    fun consumeWipeFailed() {
        _wipeFailed.value = false
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(container, container.settingsRepository)
            }
        }
    }
}
