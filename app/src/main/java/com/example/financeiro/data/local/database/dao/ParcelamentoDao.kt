package com.example.financeiro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParcelamentoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parcelamento: ParcelamentoEntity): Long

    @Update
    suspend fun update(parcelamento: ParcelamentoEntity)

    @Delete
    suspend fun delete(parcelamento: ParcelamentoEntity)

    @Query("SELECT * FROM parcelamentos WHERE id = :id")
    suspend fun getById(id: Long): ParcelamentoEntity?

    @Query("SELECT * FROM parcelamentos ORDER BY data_primeira_parcela ASC")
    fun getAll(): Flow<List<ParcelamentoEntity>>

    // Parcelamentos vinculados a uma transação
    @Query("SELECT * FROM parcelamentos WHERE transacao_principal_id = :transacaoId")
    fun getByTransacao(transacaoId: Long): Flow<List<ParcelamentoEntity>>

    // Parcelas futuras — parcelamentos com parcelas ainda não pagas
    // cuja primeira parcela cai no mês informado ou após
    @Query("""
        SELECT * FROM parcelamentos
        WHERE parcelas_pagas < total_parcelas
        AND data_primeira_parcela >= :inicioMes
        AND data_primeira_parcela < :fimMes
        ORDER BY data_primeira_parcela ASC
    """)
    fun getParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<List<ParcelamentoEntity>>

    // Todos os parcelamentos em aberto (parcelas_pagas < total_parcelas)
    @Query("""
        SELECT * FROM parcelamentos
        WHERE parcelas_pagas < total_parcelas
        ORDER BY data_primeira_parcela ASC
    """)
    fun getParcelamentosEmAberto(): Flow<List<ParcelamentoEntity>>

    @Query("""
        SELECT SUM(valor_parcela * (total_parcelas - parcelas_pagas)) FROM parcelamentos
        WHERE parcelas_pagas < total_parcelas
    """)
    fun getSomaSaldoDevedorEmAberto(): Flow<Double?>

    // Soma do valor comprometido em parcelamentos futuros no mês
    @Query("""
        SELECT SUM(valor_parcela) FROM parcelamentos
        WHERE parcelas_pagas < total_parcelas
        AND data_primeira_parcela >= :inicioMes
        AND data_primeira_parcela < :fimMes
    """)
    fun getSomaParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<Double?>
}
