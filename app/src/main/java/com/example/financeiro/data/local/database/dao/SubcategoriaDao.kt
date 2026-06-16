package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.SubcategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subcategoria: SubcategoriaEntity): Long

    @Update
    suspend fun update(subcategoria: SubcategoriaEntity)

    @Delete
    suspend fun delete(subcategoria: SubcategoriaEntity)

    @Query("SELECT * FROM subcategorias WHERE id = :id")
    suspend fun getById(id: Long): SubcategoriaEntity?

    @Query("SELECT * FROM subcategorias ORDER BY nome ASC")
    fun getAll(): Flow<List<SubcategoriaEntity>>

    @Query("SELECT * FROM subcategorias WHERE categoria_id = :categoriaId ORDER BY nome ASC")
    fun getByCategoriaId(categoriaId: Long): Flow<List<SubcategoriaEntity>>

    @Query("SELECT * FROM subcategorias WHERE categoria_id = :categoriaId AND ativa = 1 ORDER BY nome ASC")
    fun getAtivasByCategoriaId(categoriaId: Long): Flow<List<SubcategoriaEntity>>
}