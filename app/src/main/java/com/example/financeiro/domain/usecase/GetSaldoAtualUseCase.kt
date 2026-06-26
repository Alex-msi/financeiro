package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetSaldoAtualUseCase @Inject constructor(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository
) {
    operator fun invoke(ate: Long): Flow<Double> =
        combine(
            contaRepository.getSaldoTotal(),
            transacaoRepository.getAll()
        ) { saldoContas, transacoes ->
            val saldoMovimentosAteHoje = transacoes
                .filter { it.formaPagamento == "conta" || it.formaPagamento == "dinheiro" }
                .filter { it.dataCompetencia <= ate }
                .sumOf { if (it.tipo == "receita") it.valor else -it.valor }
            (saldoContas ?: 0.0) + saldoMovimentosAteHoje
        }
}
