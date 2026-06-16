package com.example.financeiro.domain.model

data class Categoria(
    val id: Long = 0,
    val nome: String,
    val tipo: String,
    val cor: String = "#607D8B",
    val criadoEm: Long
)