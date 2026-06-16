package com.example.financeiro.domain.repository

import com.example.financeiro.domain.model.Cartao
import kotlinx.coroutines.flow.Flow

interface CartaoRepository {
    suspend fun insert(cartao: Cartao): Long
    suspend fun update(cartao: Cartao)
    suspend fun delete(cartao: Cartao)
    suspend fun getById(id: Long): Cartao?
    fun getAll(): Flow<List<Cartao>>
    fun getAllAtivos(): Flow<List<Cartao>>
    fun getFaturaMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<Double?>
}