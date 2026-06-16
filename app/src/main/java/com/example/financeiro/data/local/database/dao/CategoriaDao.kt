package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: CategoriaEntity): Long

    @Update
    suspend fun update(categoria: CategoriaEntity)

    @Delete
    suspend fun delete(categoria: CategoriaEntity)

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun getById(id: Long): CategoriaEntity?

    @Query("SELECT * FROM categorias ORDER BY nome ASC")
    fun getAll(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE tipo = :tipo ORDER BY nome ASC")
    fun getByTipo(tipo: String): Flow<List<CategoriaEntity>>
}