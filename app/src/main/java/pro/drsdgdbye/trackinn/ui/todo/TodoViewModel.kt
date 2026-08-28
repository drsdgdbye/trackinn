package pro.drsdgdbye.trackinn.ui.todo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.drsdgdbye.trackinn.data.db.TrackinnDatabase
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity
import pro.drsdgdbye.trackinn.data.repository.TaskRepository

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    val tasks: MutableStateFlow<List<TaskEntity>>

    init {
        val db = TrackinnDatabase.getInstance(application)
        repository = TaskRepository(db.taskDao())
        tasks = MutableStateFlow(emptyList())
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
}
