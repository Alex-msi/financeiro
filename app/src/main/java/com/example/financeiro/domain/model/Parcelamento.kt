package com.example.financeiro.domain.model

data class Parcelamento(
    val id: Long = 0,
    val transacaoPrincipalId: Long,
    val valorParcela: Double,
    val totalParcelas: Int,
    val parcelasPagas: Int = 0,
    val dataPrimeiraParcela: Long,
    val cartaoId: Long?,
    val criadoEm: Long
)