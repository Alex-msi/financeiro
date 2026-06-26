package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PagarFaturaCartaoUseCase @Inject constructor(
    private val cartaoRepository: CartaoRepository,
    private val contaRepository: ContaRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val transacaoRepository: TransacaoRepository
) {
    suspend operator fun invoke(
        cartaoId: Long,
        inicioMes: Long,
        fimMes: Long,
        formaPagamento: String,
        contaId: Long?,
        valorPagamento: Double
    ): Result<Double> = runCatching {
        val cartao = cartaoRepository.getById(cartaoId) ?: error("Cartão não encontrado")
        require(formaPagamento in listOf("conta", "dinheiro")) { "Origem de pagamento invalida" }
        require(valorPagamento > 0.0) { "Informe um valor maior que zero" }
        val conta = if (formaPagamento == "conta") {
            val id = contaId ?: error("Selecione uma conta para pagar a fatura")
            contaRepository.getById(id) ?: error("Conta nao encontrada")
        } else {
            null
        }

        val parcelamentos = parcelamentoRepository.getParcelamentosEmAberto().first()
            .filter { it.cartaoId == cartaoId }
        val transacoes = transacaoRepository.getAll().first()
        val parcelasDaFatura = parcelamentos.mapNotNull { parcelamento ->
            val indice = RegrasFinanceiras.indiceParcelaNoMes(parcelamento, inicioMes)
            if (
                indice in 0 until parcelamento.totalParcelas &&
                RegrasFinanceiras.estaNoPeriodo(
                    RegrasFinanceiras.dataParcela(parcelamento, indice),
                    inicioMes,
                    fimMes
                )
            ) {
                parcelamento to indice
            } else {
                null
            }
        }

        val comprasAvulsas = transacoes.filter { transacao ->
            transacao.tipo == "despesa" &&
                transacao.formaPagamento == "cartao" &&
                transacao.cartaoId == cartaoId &&
                !RegrasFinanceiras.isPagamentoFatura(transacao) &&
                !transacao.parcelado &&
                RegrasFinanceiras.estaNoPeriodo(
                    RegrasFinanceiras.dataFatura(transacao, cartao),
                    inicioMes,
                    fimMes
                )
        }
        val totalFatura = parcelasDaFatura.sumOf { (parcelamento, _) -> parcelamento.valorParcela } +
            comprasAvulsas.sumOf { it.valor }
        val totalJaPago = RegrasFinanceiras.totalPagoFatura(transacoes, cartaoId, inicioMes)
        val valorAberto = (totalFatura - totalJaPago).coerceAtLeast(0.0)
        require(valorAberto > 0.0) { "Esta fatura ja foi paga" }
        require(valorPagamento <= valorAberto + 0.009) { "Valor maior que o aberto da fatura" }

        val totalPagoParaParcelas = (totalJaPago + valorPagamento - comprasAvulsas.sumOf { it.valor })
            .coerceAtLeast(0.0)
        atualizarParcelasPagas(parcelasDaFatura, totalPagoParaParcelas)

        transacaoRepository.insert(
            Transacao(
                valor = valorPagamento,
                dataCompetencia = dataPagamento(),
                tipo = "despesa",
                categoriaId = null,
                subcategoriaId = null,
                formaPagamento = formaPagamento,
                cartaoId = cartaoId,
                contaId = conta?.id,
                parcelado = false,
                numeroParcelas = 1,
                parcelaAtual = 1,
                observacao = RegrasFinanceiras.observacaoPagamentoFatura(cartao.nome, inicioMes),
                criadoEm = System.currentTimeMillis()
            )
        )

        valorPagamento
    }

    private suspend fun atualizarParcelasPagas(
        parcelasDaFatura: List<Pair<Parcelamento, Int>>,
        totalPago: Double
    ) {
        var restantePago = totalPago
        parcelasDaFatura
            .sortedWith(compareBy<Pair<Parcelamento, Int>> { RegrasFinanceiras.dataParcela(it.first, it.second) })
            .forEach { (parcelamento, indice) ->
                if (restantePago + 0.009 >= parcelamento.valorParcela) {
                    restantePago -= parcelamento.valorParcela
                    if (indice + 1 > parcelamento.parcelasPagas) {
                        parcelamentoRepository.update(
                            parcelamento.copy(parcelasPagas = indice + 1)
                        )
                    }
                }
            }
    }

    private fun dataPagamento(): Long = System.currentTimeMillis()
}
