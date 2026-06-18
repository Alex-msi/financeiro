package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSaldoTenhoUseCase @Inject constructor(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository
) {
    operator fun invoke(): Flow<Double> =
        combine(
            contaRepository.getSaldoTotal(),
            transacaoRepository.getSomaDespesasCartaoNaoParceladas(),
            parcelamentoRepository.getSomaSaldoDevedorEmAberto()
        ) { saldoContas, dividaCartoes, dividaParcelamentos ->
            (saldoContas ?: 0.0) - (dividaCartoes ?: 0.0) - (dividaParcelamentos ?: 0.0)
        }
}
