package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cartoes")
data class CartaoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    @ColumnInfo(name = "limite_total")
    val limiteTotal: Double,

    @ColumnInfo(name = "dia_fechamento")
    val diaFechamento: Int, // 1-28

    @ColumnInfo(name = "dia_vencimento")
    val diaVencimento: Int, // 1-28

    val ativo: Boolean = true,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)