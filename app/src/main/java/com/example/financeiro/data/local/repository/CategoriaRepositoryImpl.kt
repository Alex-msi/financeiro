package com.example.financeiro.data.local.repository

import com.example.financeiro.data.local.LocalDataSource
import com.example.financeiro.data.local.mapper.toDomain
import com.example.financeiro.data.local.mapper.toEntity
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Subcategoria
import com.example.financeiro.domain.repository.CategoriaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : CategoriaRepository {

    override suspend fun insert(categoria: Categoria): Long =
        localDataSource.categoriaDao.insert(categoria.toEntity())

    override suspend fun update(categoria: Categoria) =
        localDataSource.categoriaDao.update(categoria.toEntity())

    override suspend fun delete(categoria: Categoria) =
        localDataSource.categoriaDao.delete(categoria.toEntity())

    override suspend fun getById(id: Long): Categoria? =
        localDataSource.categoriaDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Categoria>> =
        localDataSource.categoriaDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByTipo(tipo: String): Flow<List<Categoria>> =
        localDataSource.categoriaDao.getByTipo(tipo).map { list -> list.map { it.toDomain() } }

    override suspend fun insertSubcategoria(subcategoria: Subcategoria): Long =
        localDataSource.subcategoriaDao.insert(subcategoria.toEntity())

    override suspend fun updateSubcategoria(subcategoria: Subcategoria) =
        localDataSource.subcategoriaDao.update(subcategoria.toEntity())

    override suspend fun deleteSubcategoria(subcategoria: Subcategoria) =
        localDataSource.subcategoriaDao.delete(subcategoria.toEntity())

    override suspend fun getSubcategoriaById(id: Long): Subcategoria? =
        localDataSource.subcategoriaDao.getById(id)?.toDomain()

    override fun getAllSubcategorias(): Flow<List<Subcategoria>> =
        localDataSource.subcategoriaDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getSubcategoriasByCategoriaId(categoriaId: Long): Flow<List<Subcategoria>> =
        localDataSource.subcategoriaDao.getByCategoriaId(categoriaId).map { list -> list.map { it.toDomain() } }

    override fun getSubcategoriasAtivasByCategoriaId(categoriaId: Long): Flow<List<Subcategoria>> =
        localDataSource.subcategoriaDao.getAtivasByCategoriaId(categoriaId).map { list -> list.map { it.toDomain() } }
}
