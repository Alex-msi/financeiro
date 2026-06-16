package com.example.financeiro.domain.model

data class Cartao(
    val id: Long = 0,
    val nome: String,
    val limiteTotal: Double,
    val diaFechamento: Int,
    val diaVencimento: Int,
    val ativo: Boolean = true,
    val criadoEm: Long
)