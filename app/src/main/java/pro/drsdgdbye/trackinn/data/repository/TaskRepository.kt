package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.TaskDao
import pro.drsdgdbye.trackinn.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    fun getAll(): Flow<List<TaskEntity>> = taskDao.getAll()

    suspend fun getById(id: Long): TaskEntity? = taskDao.getById(id)

    suspend fun create(title: String, dueDate: Long?, dueTime: Long?): Long {
        val position = taskDao.getNextPosition()
        return taskDao.insert(
            TaskEntity(
                title = title,
                dueDate = dueDate,
                dueTime = dueTime,
                position = position
            )
        )
    }

    suspend fun update(task: TaskEntity) = taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun toggleDone(task: TaskEntity) = taskDao.update(task.copy(isDone = !task.isDone, updatedAt = System.currentTimeMillis()))

    suspend fun delete(task: TaskEntity) = taskDao.delete(task)

    suspend fun deleteCompleted() = taskDao.deleteCompleted()

    suspend fun updatePositions(tasks: List<TaskEntity>) {
        tasks.forEachIndexed { index, task ->
            taskDao.update(task.copy(position = index))
        }
    }
}
