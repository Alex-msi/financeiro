package com.example.financeiro

import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasFinanceirasTest {

    private val cartao = Cartao(
        id = 1,
        nome = "Cartao teste",
        limiteTotal = 2_000.0,
        diaFechamento = 25,
        diaVencimento = 3,
        criadoEm = 0
    )

    @Test
    fun compraAntesDoFechamentoEntraNaFaturaSeguintePeloVencimento() {
        val compra = transacaoCartao(data = data(2026, Calendar.JANUARY, 5), valor = 100.0)

        val vencimento = RegrasFinanceiras.dataFatura(compra, cartao)

        assertTrue(
            RegrasFinanceiras.mesmaCompetencia(
                vencimento,
                data(2026, Calendar.FEBRUARY, 3)
            )
        )
    }

    @Test
    fun compraDepoisDoFechamentoEntraUmMesMaisTarde() {
        val compra = transacaoCartao(data = data(2026, Calendar.JANUARY, 26), valor = 100.0)

        val vencimento = RegrasFinanceiras.dataFatura(compra, cartao)

        assertTrue(
            RegrasFinanceiras.mesmaCompetencia(
                vencimento,
                data(2026, Calendar.MARCH, 3)
            )
        )
    }

    @Test
    fun pagamentoDaFaturaNaoDuplicaDespesaMensal() {
        val inicio = data(2026, Calendar.FEBRUARY, 1)
        val fim = data(2026, Calendar.MARCH, 1)
        val compra = transacaoCartao(
            id = 10,
            data = data(2026, Calendar.JANUARY, 5),
            valor = 300.0,
            parcelado = true,
            numeroParcelas = 3
        )
        val parcelamento = Parcelamento(
            id = 1,
            transacaoPrincipalId = 10,
            valorParcela = 100.0,
            totalParcelas = 3,
            parcelasPagas = 1,
            dataPrimeiraParcela = data(2026, Calendar.FEBRUARY, 3),
            cartaoId = 1,
            criadoEm = 0
        )
        val pagamento = pagamentoFatura(data(2026, Calendar.FEBRUARY, 3), 100.0)

        val totais = RegrasFinanceiras.calcularTotaisMes(
            listOf(compra, pagamento),
            listOf(parcelamento),
            inicio,
            fim
        )

        assertEquals(100.0, totais.despesas, 0.001)
    }

    @Test
    fun compraAvulsaDeixaDeSerDividaDepoisDoPagamentoDaCompetencia() {
        val compra = transacaoCartao(data = data(2026, Calendar.JANUARY, 5), valor = 250.0)
        val pagamento = pagamentoFatura(data(2026, Calendar.FEBRUARY, 3), 250.0)

        val emAberto = RegrasFinanceiras.calcularDividaComprasAvulsas(
            listOf(compra),
            listOf(cartao)
        )
        val quitada = RegrasFinanceiras.calcularDividaComprasAvulsas(
            listOf(compra, pagamento),
            listOf(cartao)
        )

        assertEquals(250.0, emAberto, 0.001)
        assertEquals(0.0, quitada, 0.001)
    }

    @Test
    fun pagamentoComDataAtualPodeQuitarFaturaDeOutraCompetencia() {
        val competenciaFatura = data(2026, Calendar.AUGUST, 1)
        val pagamento = pagamentoFatura(
            data = data(2026, Calendar.SEPTEMBER, 10),
            valor = 200.0,
            competenciaFatura = competenciaFatura
        )

        assertTrue(RegrasFinanceiras.pagamentoPertenceAFatura(pagamento, competenciaFatura))
    }

    private fun transacaoCartao(
        id: Long = 1,
        data: Long,
        valor: Double,
        parcelado: Boolean = false,
        numeroParcelas: Int = 1
    ) = Transacao(
        id = id,
        valor = valor,
        dataCompetencia = data,
        tipo = "despesa",
        categoriaId = null,
        subcategoriaId = null,
        formaPagamento = "cartao",
        cartaoId = 1,
        contaId = null,
        parcelado = parcelado,
        numeroParcelas = numeroParcelas,
        parcelaAtual = 1,
        observacao = "Compra",
        criadoEm = 0
    )

    private fun pagamentoFatura(
        data: Long,
        valor: Double,
        competenciaFatura: Long = data
    ) = Transacao(
        id = 2,
        valor = valor,
        dataCompetencia = data,
        tipo = "despesa",
        categoriaId = null,
        subcategoriaId = null,
        formaPagamento = "conta",
        cartaoId = 1,
        contaId = 1,
        parcelado = false,
        numeroParcelas = 1,
        parcelaAtual = 1,
        observacao = RegrasFinanceiras.observacaoPagamentoFatura(cartao.nome, competenciaFatura),
        criadoEm = 0
    )

    private fun data(ano: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance().apply {
            set(ano, mes, dia, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
