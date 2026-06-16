package com.example.financeiro.domain.model

data class Transacao(
    val id: Long = 0,
    val valor: Double,
    val dataCompetencia: Long,
    val tipo: String,
    val categoriaId: Long?,
    val subcategoriaId: Long?,
    val formaPagamento: String,
    val cartaoId: Long?,
    val contaId: Long?,
    val parcelado: Boolean = false,
    val numeroParcelas: Int = 1,
    val parcelaAtual: Int = 1,
    val observacao: String?,
    val criadoEm: Long
)