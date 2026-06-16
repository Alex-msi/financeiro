package com.example.financeiro.domain.model

data class Subcategoria(
    val id: Long = 0,
    val nome: String,
    val categoriaId: Long,
    val ativa: Boolean = true,
    val criadoEm: Long
)