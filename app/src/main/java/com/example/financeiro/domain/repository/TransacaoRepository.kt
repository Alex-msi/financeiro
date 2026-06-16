package com.example.financeiro.domain.repository

import com.example.financeiro.domain.model.Transacao
import kotlinx.coroutines.flow.Flow

interface TransacaoRepository {
    suspend fun insert(transacao: Transacao): Long
    suspend fun update(transacao: Transacao)
    suspend fun delete(transacao: Transacao)
    suspend fun getById(id: Long): Transacao?
    fun getAll(): Flow<List<Transacao>>
    fun getByMes(inicioMes: Long, fimMes: Long): Flow<List<Transacao>>
    fun getByCartao(cartaoId: Long): Flow<List<Transacao>>
    fun getByCartaoMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<List<Transacao>>
    fun getByCategoria(categoriaId: Long): Flow<List<Transacao>>
    fun getSomaReceitasPeriodo(inicio: Long, fim: Long): Flow<Double?>
    fun getSomaDespesasPeriodo(inicio: Long, fim: Long): Flow<Double?>
    fun getSaldoPeriodo(inicio: Long, fim: Long): Flow<Double?>
}