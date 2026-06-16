package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    val tipo: String, // "receita" ou "despesa"

    val cor: String = "#607D8B", // hex color

    val icone: String = "ic_categoria",

    val ativa: Boolean = true,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)