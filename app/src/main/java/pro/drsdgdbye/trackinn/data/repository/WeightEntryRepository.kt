package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.WeightEntryDao
import pro.drsdgdbye.trackinn.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

class WeightEntryRepository(private val weightEntryDao: WeightEntryDao) {

    fun getAll(): Flow<List<WeightEntryEntity>> = weightEntryDao.getAll()

    suspend fun add(weightKg: Double): Long {
        return weightEntryDao.insert(WeightEntryEntity(weightKg = weightKg))
    }

    suspend fun getCount(): Int = weightEntryDao.getCount()
}
