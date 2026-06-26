package com.example.financeiro.data.local.repository

import com.example.financeiro.data.local.LocalDataSource
import com.example.financeiro.data.local.mapper.toDomain
import com.example.financeiro.data.local.mapper.toEntity
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransacaoRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource
) : TransacaoRepository {

    override suspend fun insert(transacao: Transacao): Long =
        localDataSource.transacaoDao.insert(transacao.toEntity())

    override suspend fun update(transacao: Transacao) =
        localDataSource.transacaoDao.update(transacao.toEntity())

    override suspend fun delete(transacao: Transacao) =
        localDataSource.transacaoDao.delete(transacao.toEntity())

    override suspend fun getById(id: Long): Transacao? =
        localDataSource.transacaoDao.getById(id)?.toDomain()

    override fun getAll(): Flow<List<Transacao>> =
        localDataSource.transacaoDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByMes(inicioMes: Long, fimMes: Long): Flow<List<Transacao>> =
        localDataSource.transacaoDao.getByMes(inicioMes, fimMes).map { list -> list.map { it.toDomain() } }

    override fun getByCartao(cartaoId: Long): Flow<List<Transacao>> =
        localDataSource.transacaoDao.getByCartao(cartaoId).map { list -> list.map { it.toDomain() } }

    override fun getByCartaoMes(cartaoId: Long, inicioMes: Long, fimMes: Long): Flow<List<Transacao>> =
        localDataSource.transacaoDao.getByCartaoMes(cartaoId, inicioMes, fimMes).map { list -> list.map { it.toDomain() } }

    override fun getByCategoria(categoriaId: Long): Flow<List<Transacao>> =
        localDataSource.transacaoDao.getByCategoria(categoriaId).map { list -> list.map { it.toDomain() } }

    override fun getSomaReceitasPeriodo(inicio: Long, fim: Long): Flow<Double?> =
        localDataSource.transacaoDao.getSomaReceitasPeriodo(inicio, fim)

    override fun getSomaDespesasPeriodo(inicio: Long, fim: Long): Flow<Double?> =
        localDataSource.transacaoDao.getSomaDespesasPeriodo(inicio, fim)

    override fun getSomaDespesasNaoParceladasPeriodo(inicio: Long, fim: Long): Flow<Double?> =
        localDataSource.transacaoDao.getSomaDespesasNaoParceladasPeriodo(inicio, fim)

    override fun getSomaDespesasCartaoNaoParceladas(): Flow<Double?> =
        localDataSource.transacaoDao.getSomaDespesasCartaoNaoParceladas()

    override fun getSaldoTransacoesConta(): Flow<Double?> =
        localDataSource.transacaoDao.getSaldoTransacoesConta()

    override fun getSaldoPeriodo(inicio: Long, fim: Long): Flow<Double?> =
        localDataSource.transacaoDao.getSaldoPeriodo(inicio, fim)
}
