package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.TransacaoRepository
import javax.inject.Inject

class SalvarTransacaoUseCase @Inject constructor(
    private val transacaoRepository: TransacaoRepository,
    private val gerarParcelasUseCase: GerarParcelasUseCase
) {
    suspend operator fun invoke(transacao: Transacao): Result<Unit> = runCatching {
        validar(transacao)

        val id = transacaoRepository.insert(transacao)

        if (transacao.parcelado && transacao.numeroParcelas > 1) {
            val transacaoComId = transacao.copy(id = id)
            gerarParcelasUseCase(transacaoComId).getOrThrow()
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