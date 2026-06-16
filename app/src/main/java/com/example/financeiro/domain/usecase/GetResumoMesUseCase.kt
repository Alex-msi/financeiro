package com.example.financeiro.domain.usecase

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
    private val transacaoRepository: TransacaoRepository
) {
    operator fun invoke(inicioMes: Long, fimMes: Long): Flow<ResumoMes> =
        combine(
            transacaoRepository.getSomaReceitasPeriodo(inicioMes, fimMes),
            transacaoRepository.getSomaDespesasPeriodo(inicioMes, fimMes)
        ) { receitas, despesas ->
            val r = receitas ?: 0.0
            val d = despesas ?: 0.0
            ResumoMes(
                totalReceitas = r,
                totalDespesas = d,
                saldo = r - d
            )
        }
}