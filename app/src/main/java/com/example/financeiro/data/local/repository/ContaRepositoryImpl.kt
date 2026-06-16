package com.example.financeiro.data.local.repository

import com.example.financeiro.data.local.LocalDataSource
import com.example.financeiro.data.local.mapper.toDomain
import com.example.financeiro.data.local.mapper.toEntity
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.repository.ContaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContaRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : ContaRepository {

    override suspend fun insert(conta: Conta): Long =
        localDataSource.contaDao.insert(conta.toEntity())

    override suspend fun update(conta: Conta) =
        localDataSource.contaDao.update(conta.toEntity())

    override suspend fun delete(conta: Conta) =
        localDataSource.contaDao.delete(conta.toEntity())

    override suspend fun getById(id: Long): Conta? =
        localDataSource.contaDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Conta>> =
        localDataSource.contaDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getAllAtivas(): Flow<List<Conta>> =
        localDataSource.contaDao.getAllAtivas().map { list -> list.map { it.toDomain() } }

    override fun getSaldoTotal(): Flow<Double?> =
        localDataSource.contaDao.getSaldoTotal()
}