package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class ResumoMes(
    val totalReceitas: Double,
    val totalDespesas: Double,
    val saldo: Double
)

class GetResumoMesUseCase @Inject constructor(
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository
) {
    operator fun invoke(inicioMes: Long, fimMes: Long): Flow<ResumoMes> =
        combine(
            transacaoRepository.getAll(),
            parcelamentoRepository.getAll()
        ) { transacoes, parcelamentos ->
            val totais = RegrasFinanceiras.calcularTotaisMes(
                transacoes,
                parcelamentos,
                inicioMes,
                fimMes
            )
            ResumoMes(
                totalReceitas = totais.receitas,
                totalDespesas = totais.despesas,
                saldo = totais.saldo
            )
        }
}
