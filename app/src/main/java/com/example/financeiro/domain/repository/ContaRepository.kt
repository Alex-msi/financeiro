package com.example.financeiro.domain.repository

import com.example.financeiro.domain.model.Conta
import kotlinx.coroutines.flow.Flow

interface ContaRepository {
    suspend fun insert(conta: Conta): Long
    suspend fun update(conta: Conta)
    suspend fun delete(conta: Conta)
    suspend fun getById(id: Long): Conta?
    fun getAll(): Flow<List<Conta>>
    fun getAllAtivas(): Flow<List<Conta>>
    fun getSaldoTotal(): Flow<Double?>
}