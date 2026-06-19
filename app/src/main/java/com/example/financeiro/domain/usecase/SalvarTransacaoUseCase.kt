package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.first
import kotlin.math.min
import javax.inject.Inject

class SalvarTransacaoUseCase @Inject constructor(
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val gerarParcelasUseCase: GerarParcelasUseCase
) {
    suspend operator fun invoke(transacao: Transacao): Result<Unit> = runCatching {
        validar(transacao)

        val id = if (transacao.id > 0) {
            transacaoRepository.update(transacao)
            transacao.id
        } else {
            transacaoRepository.insert(transacao)
        }

        val transacaoComId = transacao.copy(id = id)
        sincronizarParcelamento(transacaoComId)
    }

    private suspend fun sincronizarParcelamento(transacao: Transacao) {
        val parcelamentoExistente = parcelamentoRepository.getByTransacao(transacao.id).first().firstOrNull()
        if (transacao.parcelado && transacao.numeroParcelas > 1) {
            if (parcelamentoExistente == null) {
                gerarParcelasUseCase(transacao).getOrThrow()
            } else {
                parcelamentoRepository.update(
                    parcelamentoExistente.copy(
                        valorParcela = transacao.valor / transacao.numeroParcelas,
                        totalParcelas = transacao.numeroParcelas,
                        parcelasPagas = min(parcelamentoExistente.parcelasPagas, transacao.numeroParcelas),
                        cartaoId = transacao.cartaoId
                    )
                )
            }
        } else if (parcelamentoExistente != null) {
            parcelamentoRepository.delete(parcelamentoExistente)
        }
    }

    private fun validar(transacao: Transacao) {
        require(transacao.valor > 0) { "Valor deve ser maior que zero" }
        require(transacao.tipo in listOf("receita", "despesa")) { "Tipo inválido" }
        require(transacao.formaPagamento in listOf("cartao", "conta", "dinheiro")) { "Forma de pagamento inválida" }

        if (transacao.formaPagamento == "cartao") {
            require(transacao.cartaoId != null) { "Cartão obrigatório para pagamento com cartão" }
        }

        if (transacao.formaPagamento == "conta") {
            require(transacao.contaId != null) { "Conta obrigatória para pagamento com conta" }
        }

        if (transacao.parcelado) {
            require(transacao.numeroParcelas > 1) { "Parcelado exige mais de 1 parcela" }
            require(transacao.formaPagamento == "cartao") { "Parcelamento só é permitido no cartão" }
        }
    }
}
