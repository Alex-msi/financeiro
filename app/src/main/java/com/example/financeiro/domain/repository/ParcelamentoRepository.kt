package com.example.financeiro.domain.repository

import com.example.financeiro.domain.model.Parcelamento
import kotlinx.coroutines.flow.Flow

interface ParcelamentoRepository {
    suspend fun insert(parcelamento: Parcelamento): Long
    suspend fun update(parcelamento: Parcelamento)
    suspend fun delete(parcelamento: Parcelamento)
    suspend fun getById(id: Long): Parcelamento?
    fun getAll(): Flow<List<Parcelamento>>
    fun getByTransacao(transacaoId: Long): Flow<List<Parcelamento>>
    fun getParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<List<Parcelamento>>
    fun getParcelamentosEmAberto(): Flow<List<Parcelamento>>
    fun getSomaParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<Double?>
}