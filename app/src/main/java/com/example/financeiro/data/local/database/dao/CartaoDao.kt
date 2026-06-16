package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.CartaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartaoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cartao: CartaoEntity): Long

    @Update
    suspend fun update(cartao: CartaoEntity)

    @Delete
    suspend fun delete(cartao: CartaoEntity)

    @Query("SELECT * FROM cartoes WHERE id = :id")
    suspend fun getById(id: Long): CartaoEntity?

    @Query("SELECT * FROM cartoes ORDER BY nome ASC")
    fun getAll(): Flow<List<CartaoEntity>>

    @Query("SELECT * FROM cartoes WHERE ativo = 1 ORDER BY nome ASC")
    fun getAllAtivos(): Flow<List<CartaoEntity>>

    // Soma as transações do cartão no mês/ano informado (epoch millis do início e fim do mês)
    @Query("""
        SELECT SUM(t.valor) FROM transacoes t
        WHERE t.cartao_id = :cartaoId
        AND t.tipo = 'despesa'
        AND t.data_competencia >= :inicioMes
        AND t.data_competencia < :fimMes
    """)
    fun getFaturaMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<Double?>
}