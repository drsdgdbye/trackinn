package pro.drsdgdbye.trackinn.data.repository

import pro.drsdgdbye.trackinn.data.db.dao.ProductDao
import pro.drsdgdbye.trackinn.data.db.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val productDao: ProductDao) {

    fun search(query: String): Flow<List<ProductEntity>> = productDao.search(query)

    fun getAll(): Flow<List<ProductEntity>> = productDao.getAll()

    suspend fun getById(id: Long): ProductEntity? = productDao.getById(id)

    suspend fun create(product: ProductEntity): Long = productDao.insert(product)

    suspend fun update(product: ProductEntity) = productDao.update(product)

    suspend fun delete(product: ProductEntity) = productDao.delete(product)
}
