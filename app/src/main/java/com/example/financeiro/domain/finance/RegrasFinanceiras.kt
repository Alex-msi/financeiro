package com.example.financeiro.domain.finance

import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import java.util.Calendar

object RegrasFinanceiras {

    const val PREFIXO_PAGAMENTO_FATURA = "Pagamento fatura "
    private const val MARCADOR_COMPETENCIA_FATURA = " | competencia="

    fun isPagamentoFatura(transacao: Transacao): Boolean =
        transacao.formaPagamento in listOf("conta", "dinheiro") &&
            transacao.tipo == "despesa" &&
            transacao.observacao?.startsWith(PREFIXO_PAGAMENTO_FATURA) == true

    fun observacaoPagamentoFatura(nomeCartao: String, competenciaFatura: Long): String {
        val competencia = Calendar.getInstance().apply { timeInMillis = competenciaFatura }
        return "$PREFIXO_PAGAMENTO_FATURA$nomeCartao$MARCADOR_COMPETENCIA_FATURA" +
            "%04d-%02d".format(
                competencia.get(Calendar.YEAR),
                competencia.get(Calendar.MONTH) + 1
            )
    }

    fun pagamentoPertenceAFatura(transacao: Transacao, competenciaFatura: Long): Boolean {
        if (!isPagamentoFatura(transacao)) return false
        val competenciaMarcada = competenciaMarcadaPagamento(transacao)
        return if (competenciaMarcada != null) {
            val competencia = Calendar.getInstance().apply { timeInMillis = competenciaFatura }
            competenciaMarcada.first == competencia.get(Calendar.YEAR) &&
                competenciaMarcada.second == competencia.get(Calendar.MONTH)
        } else {
            mesmaCompetencia(transacao.dataCompetencia, competenciaFatura)
        }
    }

    fun totalPagoFatura(
        transacoes: List<Transacao>,
        cartaoId: Long,
        competenciaFatura: Long
    ): Double =
        transacoes
            .filter { it.cartaoId == cartaoId && pagamentoPertenceAFatura(it, competenciaFatura) }
            .sumOf { it.valor }

    fun descricaoVisivel(transacao: Transacao): String? {
        val observacao = transacao.observacao ?: return null
        val marcadorIndex = observacao.indexOf(MARCADOR_COMPETENCIA_FATURA)
        return if (marcadorIndex >= 0) {
            observacao.substring(0, marcadorIndex)
        } else {
            observacao
        }
    }

    fun dataFatura(transacao: Transacao, cartao: Cartao): Long {
        val compra = Calendar.getInstance().apply { timeInMillis = transacao.dataCompetencia }
        val passouFechamento = compra.get(Calendar.DAY_OF_MONTH) > cartao.diaFechamento
        val vencimentoNoMesSeguinte = cartao.diaVencimento <= cartao.diaFechamento
        return Calendar.getInstance().apply {
            timeInMillis = transacao.dataCompetencia
            add(
                Calendar.MONTH,
                (if (passouFechamento) 1 else 0) + (if (vencimentoNoMesSeguinte) 1 else 0)
            )
            set(Calendar.DAY_OF_MONTH, cartao.diaVencimento)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun indiceParcelaNoMes(parcelamento: Parcelamento, referenciaMes: Long): Int {
        val primeira = Calendar.getInstance().apply {
            timeInMillis = parcelamento.dataPrimeiraParcela
        }
        val destino = Calendar.getInstance().apply { timeInMillis = referenciaMes }
        return (destino.get(Calendar.YEAR) - primeira.get(Calendar.YEAR)) * 12 +
            destino.get(Calendar.MONTH) - primeira.get(Calendar.MONTH)
    }

    fun dataParcela(parcelamento: Parcelamento, indice: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = parcelamento.dataPrimeiraParcela
            add(Calendar.MONTH, indice)
        }.timeInMillis

    fun estaNoPeriodo(data: Long, inicio: Long, fim: Long): Boolean =
        data >= inicio && data < fim

    fun mesmaCompetencia(primeiraData: Long, segundaData: Long): Boolean {
        val primeira = Calendar.getInstance().apply { timeInMillis = primeiraData }
        val segunda = Calendar.getInstance().apply { timeInMillis = segundaData }
        return primeira.get(Calendar.YEAR) == segunda.get(Calendar.YEAR) &&
            primeira.get(Calendar.MONTH) == segunda.get(Calendar.MONTH)
    }

    private fun competenciaMarcadaPagamento(transacao: Transacao): Pair<Int, Int>? {
        val observacao = transacao.observacao ?: return null
        val marcadorIndex = observacao.indexOf(MARCADOR_COMPETENCIA_FATURA)
        if (marcadorIndex < 0) return null
        val competencia = observacao
            .substring(marcadorIndex + MARCADOR_COMPETENCIA_FATURA.length)
            .take(7)
        val ano = competencia.substringBefore("-").toIntOrNull() ?: return null
        val mes = competencia.substringAfter("-", "").toIntOrNull()?.minus(1) ?: return null
        return ano to mes
    }

    fun calcularTotaisMes(
        transacoes: List<Transacao>,
        parcelamentos: List<Parcelamento>,
        inicioMes: Long,
        fimMes: Long
    ): TotaisMes {
        val transacoesMes = transacoes.filter {
            estaNoPeriodo(it.dataCompetencia, inicioMes, fimMes)
        }
        val receitas = transacoesMes.filter { it.tipo == "receita" }.sumOf { it.valor }
        val despesasDiretas = transacoesMes
            .filter { it.tipo == "despesa" && !it.parcelado }
            .filterNot(::isPagamentoFatura)
            .sumOf { it.valor }
        val parcelasDoMes = parcelamentos.sumOf { parcelamento ->
            val indice = indiceParcelaNoMes(parcelamento, inicioMes)
            if (
                indice in 0 until parcelamento.totalParcelas &&
                estaNoPeriodo(dataParcela(parcelamento, indice), inicioMes, fimMes)
            ) {
                parcelamento.valorParcela
            } else {
                0.0
            }
        }
        return TotaisMes(
            receitas = receitas,
            despesas = despesasDiretas + parcelasDoMes
        )
    }

    fun calcularDividaComprasAvulsas(
        transacoes: List<Transacao>,
        cartoes: List<Cartao>
    ): Double {
        val pagamentos = transacoes.filter(::isPagamentoFatura)
        val comprasPorFatura = transacoes
            .filter {
                it.tipo == "despesa" &&
                    it.formaPagamento == "cartao" &&
                    !it.parcelado
            }
            .map { compra ->
                val cartao = cartoes.firstOrNull { it.id == compra.cartaoId }
                val competencia = cartao?.let { dataFatura(compra, it) } ?: compra.dataCompetencia
                CompraAvulsaFatura(compra.cartaoId, competencia) to compra
            }
            .groupBy({ it.first }, { it.second })
        return comprasPorFatura.entries.sumOf { (fatura, compras) ->
            val cartaoId = fatura.cartaoId ?: return@sumOf compras.sumOf { it.valor }
            val totalCompras = compras.sumOf { it.valor }
            val totalPago = pagamentos
                .filter { it.cartaoId == cartaoId && pagamentoPertenceAFatura(it, fatura.competencia) }
                .sumOf { it.valor }
            (totalCompras - totalPago).coerceAtLeast(0.0)
        }
    }

    private data class CompraAvulsaFatura(val cartaoId: Long?, val competencia: Long)
}

data class TotaisMes(
    val receitas: Double,
    val despesas: Double
) {
    val saldo: Double get() = receitas - despesas
}
