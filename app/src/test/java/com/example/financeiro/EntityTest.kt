package com.example.financeiro

import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import com.example.financeiro.data.local.database.entity.TransacaoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransacaoEntityTest {

    @Test
    fun transacaoEntity_criadaComValoresObrigatoriosCorretos() {
        val transacao = TransacaoEntity(
            valor = 150.0,
            dataCompetencia = 1_700_000_000_000L,
            tipo = "despesa",
            formaPagamento = "cartao",
            categoriaId = 1L,
            subcategoriaId = 2L,
            cartaoId = 1L,
            contaId = null
        )

        assertEquals(150.0, transacao.valor, 0.0)
        assertEquals(1_700_000_000_000L, transacao.dataCompetencia)
        assertEquals("despesa", transacao.tipo)
        assertEquals("cartao", transacao.formaPagamento)
        assertEquals(1L, transacao.categoriaId)
        assertEquals(2L, transacao.subcategoriaId)
        assertEquals(1L, transacao.cartaoId)
        assertNull(transacao.contaId)
    }

    @Test
    fun transacaoEntity_valoresDefaultCorretos() {
        val transacao = TransacaoEntity(
            valor = 50.0,
            dataCompetencia = 1_700_000_000_000L,
            tipo = "receita",
            formaPagamento = "conta",
            categoriaId = null,
            subcategoriaId = null,
            cartaoId = null,
            contaId = 1L
        )

        assertEquals(0L, transacao.id)
        assertFalse(transacao.parcelado)
        assertEquals(1, transacao.numeroParcelas)
        assertEquals(1, transacao.parcelaAtual)
        assertNull(transacao.observacao)
    }

    @Test
    fun transacaoEntity_criadaParcelada() {
        val transacao = TransacaoEntity(
            valor = 1200.0,
            dataCompetencia = 1_700_000_000_000L,
            tipo = "despesa",
            formaPagamento = "cartao",
            categoriaId = 3L,
            subcategoriaId = null,
            cartaoId = 2L,
            contaId = null,
            parcelado = true,
            numeroParcelas = 12,
            parcelaAtual = 1
        )

        assertTrue(transacao.parcelado)
        assertEquals(12, transacao.numeroParcelas)
        assertEquals(1, transacao.parcelaAtual)
    }

    @Test
    fun transacaoEntity_observacaoNaoNula() {
        val transacao = TransacaoEntity(
            valor = 30.0,
            dataCompetencia = 1_700_000_000_000L,
            tipo = "despesa",
            formaPagamento = "dinheiro",
            categoriaId = null,
            subcategoriaId = null,
            cartaoId = null,
            contaId = null,
            observacao = "Almoço com cliente"
        )

        assertEquals("Almoço com cliente", transacao.observacao)
    }

    @Test
    fun transacaoEntity_igualdadeViaCopy() {
        val t1 = TransacaoEntity(
            id = 1L,
            valor = 99.90,
            dataCompetencia = 1_700_000_000_000L,
            tipo = "despesa",
            formaPagamento = "conta",
            categoriaId = 1L,
            subcategoriaId = 1L,
            cartaoId = null,
            contaId = 1L,
            criadoEm = 1_700_000_000_000L
        )

        assertEquals(t1, t1.copy())
    }
}

class ParcelamentoEntityTest {

    @Test
    fun parcelamentoEntity_criadoComValoresObrigatoriosCorretos() {
        val parcelamento = ParcelamentoEntity(
            transacaoPrincipalId = 1L,
            valorParcela = 100.0,
            totalParcelas = 12,
            dataPrimeiraParcela = 1_700_000_000_000L,
            cartaoId = 1L
        )

        assertEquals(1L, parcelamento.transacaoPrincipalId)
        assertEquals(100.0, parcelamento.valorParcela, 0.0)
        assertEquals(12, parcelamento.totalParcelas)
        assertEquals(1_700_000_000_000L, parcelamento.dataPrimeiraParcela)
        assertEquals(1L, parcelamento.cartaoId)
    }

    @Test
    fun parcelamentoEntity_valoresDefaultCorretos() {
        val parcelamento = ParcelamentoEntity(
            transacaoPrincipalId = 2L,
            valorParcela = 50.0,
            totalParcelas = 6,
            dataPrimeiraParcela = 1_700_000_000_000L,
            cartaoId = null
        )

        assertEquals(0L, parcelamento.id)
        assertEquals(0, parcelamento.parcelasPagas)
    }

    @Test
    fun parcelamentoEntity_cartaoNulo() {
        val parcelamento = ParcelamentoEntity(
            transacaoPrincipalId = 3L,
            valorParcela = 200.0,
            totalParcelas = 3,
            dataPrimeiraParcela = 1_700_000_000_000L,
            cartaoId = null
        )

        assertNull(parcelamento.cartaoId)
    }

    @Test
    fun parcelamentoEntity_atualizaParcelasPagasViaCopy() {
        val parcelamento = ParcelamentoEntity(
            transacaoPrincipalId = 1L,
            valorParcela = 100.0,
            totalParcelas = 10,
            dataPrimeiraParcela = 1_700_000_000_000L,
            cartaoId = 1L,
            parcelasPagas = 0
        )

        val atualizado = parcelamento.copy(parcelasPagas = 5)

        assertEquals(5, atualizado.parcelasPagas)
        assertEquals(10, atualizado.totalParcelas)
    }

    @Test
    fun parcelamentoEntity_igualdadeViaCopy() {
        val p1 = ParcelamentoEntity(
            id = 1L,
            transacaoPrincipalId = 1L,
            valorParcela = 100.0,
            totalParcelas = 12,
            parcelasPagas = 3,
            dataPrimeiraParcela = 1_700_000_000_000L,
            cartaoId = 1L,
            criadoEm = 1_700_000_000_000L
        )

        assertEquals(p1, p1.copy())
    }
}