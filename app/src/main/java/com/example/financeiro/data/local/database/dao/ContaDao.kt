package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.ContaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conta: ContaEntity): Long

    @Update
    suspend fun update(conta: ContaEntity)

    @Delete
    suspend fun delete(conta: ContaEntity)

    @Query("SELECT * FROM contas WHERE id = :id")
    suspend fun getById(id: Long): ContaEntity?

    @Query("SELECT * FROM contas ORDER BY nome ASC")
    fun getAll(): Flow<List<ContaEntity>>

    @Query("SELECT * FROM contas WHERE ativa = 1 ORDER BY nome ASC")
    fun getAllAtivas(): Flow<List<ContaEntity>>

    @Query("SELECT SUM(saldo_atual) FROM contas WHERE ativa = 1")
    fun getSaldoTotal(): Flow<Double?>
}