package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.TransacaoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransacaoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transacao: TransacaoEntity): Long

    @Update
    suspend fun update(transacao: TransacaoEntity)

    @Delete
    suspend fun delete(transacao: TransacaoEntity)

    @Query("SELECT * FROM transacoes WHERE id = :id")
    suspend fun getById(id: Long): TransacaoEntity?

    @Query("SELECT * FROM transacoes ORDER BY data_competencia DESC")
    fun getAll(): Flow<List<TransacaoEntity>>

    // Transações do mês (início e fim em epoch millis)
    @Query("""
        SELECT * FROM transacoes
        WHERE data_competencia >= :inicioMes
        AND data_competencia < :fimMes
        ORDER BY data_competencia DESC
    """)
    fun getByMes(inicioMes: Long, fimMes: Long): Flow<List<TransacaoEntity>>

    // Transações por cartão
    @Query("""
        SELECT * FROM transacoes
        WHERE cartao_id = :cartaoId
        ORDER BY data_competencia DESC
    """)
    fun getByCartao(cartaoId: Long): Flow<List<TransacaoEntity>>

    // Transações por cartão no mês
    @Query("""
        SELECT * FROM transacoes
        WHERE cartao_id = :cartaoId
        AND data_competencia >= :inicioMes
        AND data_competencia < :fimMes
        ORDER BY data_competencia DESC
    """)
    fun getByCartaoMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<List<TransacaoEntity>>

    // Transações por categoria
    @Query("""
        SELECT * FROM transacoes
        WHERE categoria_id = :categoriaId
        ORDER BY data_competencia DESC
    """)
    fun getByCategoria(categoriaId: Long): Flow<List<TransacaoEntity>>

    // Soma de receitas no período
    @Query("""
        SELECT SUM(valor) FROM transacoes
        WHERE tipo = 'receita'
        AND data_competencia >= :inicio
        AND data_competencia < :fim
    """)
    fun getSomaReceitasPeriodo(inicio: Long, fim: Long): Flow<Double?>

    // Soma de despesas no período
    @Query("""
        SELECT SUM(valor) FROM transacoes
        WHERE tipo = 'despesa'
        AND data_competencia >= :inicio
        AND data_competencia < :fim
    """)
    fun getSomaDespesasPeriodo(inicio: Long, fim: Long): Flow<Double?>

    @Query("""
        SELECT SUM(valor) FROM transacoes
        WHERE tipo = 'despesa'
        AND parcelado = 0
        AND data_competencia >= :inicio
        AND data_competencia < :fim
    """)
    fun getSomaDespesasNaoParceladasPeriodo(inicio: Long, fim: Long): Flow<Double?>

    @Query("""
        SELECT SUM(valor) FROM transacoes
        WHERE tipo = 'despesa'
        AND forma_pagamento = 'cartao'
        AND parcelado = 0
    """)
    fun getSomaDespesasCartaoNaoParceladas(): Flow<Double?>

    @Query("""
        SELECT SUM(CASE WHEN tipo = 'receita' THEN valor ELSE -valor END)
        FROM transacoes
        WHERE forma_pagamento = 'conta'
    """)
    fun getSaldoTransacoesConta(): Flow<Double?>

    // Saldo do período (receitas - despesas)
    @Query("""
        SELECT SUM(CASE WHEN tipo = 'receita' THEN valor ELSE -valor END)
        FROM transacoes
        WHERE data_competencia >= :inicio
        AND data_competencia < :fim
    """)
    fun getSaldoPeriodo(inicio: Long, fim: Long): Flow<Double?>
}
