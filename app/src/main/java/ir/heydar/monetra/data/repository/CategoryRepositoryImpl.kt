package ir.heydar.monetra.data.repository

import ir.heydar.monetra.data.local.dao.CategoryDao
import ir.heydar.monetra.data.mapper.toDomain
import ir.heydar.monetra.data.mapper.toEntity
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())
}
