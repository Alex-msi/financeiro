package com.example.financeiro.data.local.repository

import com.example.financeiro.data.local.LocalDataSource
import com.example.financeiro.data.local.mapper.toDomain
import com.example.financeiro.data.local.mapper.toEntity
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.repository.CartaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartaoRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : CartaoRepository {

    override suspend fun insert(cartao: Cartao): Long =
        localDataSource.cartaoDao.insert(cartao.toEntity())

    override suspend fun update(cartao: Cartao) =
        localDataSource.cartaoDao.update(cartao.toEntity())

    override suspend fun delete(cartao: Cartao) =
        localDataSource.cartaoDao.delete(cartao.toEntity())

    override suspend fun getById(id: Long): Cartao? =
        localDataSource.cartaoDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Cartao>> =
        localDataSource.cartaoDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getAllAtivos(): Flow<List<Cartao>> =
        localDataSource.cartaoDao.getAllAtivos().map { list -> list.map { it.toDomain() } }

    override fun getFaturaMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<Double?> =
        localDataSource.cartaoDao.getFaturaMes(cartaoId, inicioMes, fimMes)
}