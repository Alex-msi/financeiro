package com.example.financeiro.domain.model

data class Conta(
    val id: Long = 0,
    val nome: String,
    val tipo: String,
    val saldoInicial: Double,
    val saldoAtual: Double,
    val ativa: Boolean = true,
    val criadoEm: Long
)