package com.example.financeiro.domain.repository

import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Subcategoria
import kotlinx.coroutines.flow.Flow

interface CategoriaRepository {
    suspend fun insert(categoria: Categoria): Long
    suspend fun update(categoria: Categoria)
    suspend fun delete(categoria: Categoria)
    suspend fun getById(id: Long): Categoria?
    fun getAll(): Flow<List<Categoria>>
    fun getByTipo(tipo: String): Flow<List<Categoria>>

    suspend fun insertSubcategoria(subcategoria: Subcategoria): Long
    suspend fun updateSubcategoria(subcategoria: Subcategoria)
    suspend fun deleteSubcategoria(subcategoria: Subcategoria)
    suspend fun getSubcategoriaById(id: Long): Subcategoria?
    fun getAllSubcategorias(): Flow<List<Subcategoria>>
    fun getSubcategoriasByCategoriaId(categoriaId: Long): Flow<List<Subcategoria>>
    fun getSubcategoriasAtivasByCategoriaId(categoriaId: Long): Flow<List<Subcategoria>>
}
