package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contas")
data class ContaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    val tipo: String, // "corrente", "poupanca", "investimento", "dinheiro"

    @ColumnInfo(name = "saldo_inicial")
    val saldoInicial: Double,

    @ColumnInfo(name = "saldo_atual")
    val saldoAtual: Double,

    val ativa: Boolean = true,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)