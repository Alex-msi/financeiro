package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSaldoTenhoUseCase @Inject constructor(
    private val contaRepository: ContaRepository,
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val cartaoRepository: CartaoRepository
) {
    operator fun invoke(ate: Long): Flow<Double> =
        combine(
            contaRepository.getSaldoTotal(),
            transacaoRepository.getAll(),
            parcelamentoRepository.getAll(),
            cartaoRepository.getAll()
        ) { saldoContas, transacoes, parcelamentos, cartoes ->
            val saldoTransacoesDisponiveis = transacoes
                .filter { it.formaPagamento == "conta" || it.formaPagamento == "dinheiro" }
                .filter { it.dataCompetencia <= ate }
                .sumOf { if (it.tipo == "receita") it.valor else -it.valor }
            val dividaCartoes =
                calcularDividaComprasAvulsasAte(transacoes, cartoes, ate)
            val dividaParcelamentos = parcelamentos.sumOf { parcelamento ->
                (parcelamento.parcelasPagas until parcelamento.totalParcelas).sumOf { indice ->
                    if (RegrasFinanceiras.dataParcela(parcelamento, indice) <= ate) {
                        parcelamento.valorParcela
                    } else {
                        0.0
                    }
                }
            }
            (saldoContas ?: 0.0) +
                saldoTransacoesDisponiveis -
                dividaCartoes -
                dividaParcelamentos
        }

    private fun calcularDividaComprasAvulsasAte(
        transacoes: List<Transacao>,
        cartoes: List<Cartao>,
        ate: Long
    ): Double {
        val pagamentos = transacoes.filter(::isPagamentoFaturaAte).filter { it.dataCompetencia <= ate }
        val comprasPorFatura = transacoes
            .filter {
                it.tipo == "despesa" &&
                    it.formaPagamento == "cartao" &&
                    !it.parcelado
            }
            .mapNotNull { compra ->
                val cartao = cartoes.firstOrNull { it.id == compra.cartaoId }
                    ?: return@mapNotNull if (compra.dataCompetencia <= ate) {
                        CompraAvulsaFatura(compra.cartaoId, compra.dataCompetencia) to compra
                    } else {
                        null
                    }
                val competencia = RegrasFinanceiras.dataFatura(compra, cartao)
                if (competencia <= ate) CompraAvulsaFatura(cartao.id, competencia) to compra else null
            }
            .groupBy({ it.first }, { it.second })

        return comprasPorFatura.entries.sumOf { (fatura, compras) ->
            val cartaoId = fatura.cartaoId ?: return@sumOf compras.sumOf { it.valor }
            val totalCompras = compras.sumOf { it.valor }
            val totalPago = pagamentos
                .filter {
                    it.cartaoId == cartaoId &&
                        RegrasFinanceiras.pagamentoPertenceAFatura(it, fatura.competencia)
                }
                .sumOf { it.valor }
            (totalCompras - totalPago).coerceAtLeast(0.0)
        }
    }

    private fun isPagamentoFaturaAte(transacao: Transacao): Boolean =
        RegrasFinanceiras.isPagamentoFatura(transacao)

    private data class CompraAvulsaFatura(val cartaoId: Long?, val competencia: Long)
}
