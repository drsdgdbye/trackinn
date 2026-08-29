package pro.drsdgdbye.trackinn.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity
import pro.drsdgdbye.trackinn.data.di.appContainer
import pro.drsdgdbye.trackinn.data.repository.TaskRepository
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository

class TodoViewModel(
    private val repository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tasks: MutableStateFlow<List<TaskEntity>> = MutableStateFlow(emptyList())

    val completedTaskColor = settingsRepository.completedTaskColor
    val deadlineSafeColor = settingsRepository.deadlineSafeColor
    val deadlineWarningColor = settingsRepository.deadlineWarningColor
    val deadlineDangerColor = settingsRepository.deadlineDangerColor

    init {
        viewModelScope.launch {
            repository.getAll().collect { tasks.value = it }
        }
    }

    fun toggleDone(task: TaskEntity) {
        viewModelScope.launch { repository.toggleDone(task) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun deleteCompleted() {
        viewModelScope.launch { repository.deleteCompleted() }
    }

    fun createTask(title: String, dueDate: Long?, dueTime: Long?, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.create(title, dueDate, dueTime)
            onComplete(id)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch { repository.update(task) }
    }

    fun updatePositions(taskList: List<TaskEntity>) {
        viewModelScope.launch { repository.updatePositions(taskList) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                TodoViewModel(container.taskRepository, container.settingsRepository)
            }
        }
    }
}
