package com.example.financeiro.data.local.repository

import com.example.financeiro.data.local.LocalDataSource
import com.example.financeiro.data.local.mapper.toDomain
import com.example.financeiro.data.local.mapper.toEntity
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.repository.ParcelamentoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParcelamentoRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : ParcelamentoRepository {

    override suspend fun insert(parcelamento: Parcelamento): Long =
        localDataSource.parcelamentoDao.insert(parcelamento.toEntity())

    override suspend fun update(parcelamento: Parcelamento) =
        localDataSource.parcelamentoDao.update(parcelamento.toEntity())

    override suspend fun delete(parcelamento: Parcelamento) =
        localDataSource.parcelamentoDao.delete(parcelamento.toEntity())

    override suspend fun getById(id: Long): Parcelamento? =
        localDataSource.parcelamentoDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Parcelamento>> =
        localDataSource.parcelamentoDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByTransacao(transacaoId: Long): Flow<List<Parcelamento>> =
        localDataSource.parcelamentoDao.getByTransacao(transacaoId).map { list -> list.map { it.toDomain() } }

    override fun getParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<List<Parcelamento>> =
        localDataSource.parcelamentoDao.getParcelasFuturasPorMes(inicioMes, fimMes).map { list -> list.map { it.toDomain() } }

    override fun getParcelamentosEmAberto(): Flow<List<Parcelamento>> =
        localDataSource.parcelamentoDao.getParcelamentosEmAberto().map { list -> list.map { it.toDomain() } }

    override fun getSomaParcelasFuturasPorMes(inicioMes: Long, fimMes: Long): Flow<Double?> =
        localDataSource.parcelamentoDao.getSomaParcelasFuturasPorMes(inicioMes, fimMes)
}