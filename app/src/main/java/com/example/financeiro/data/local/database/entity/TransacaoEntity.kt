package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transacoes",
    foreignKeys = [
        ForeignKey(
            entity = CartaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartao_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ContaEntity::class,
            parentColumns = ["id"],
            childColumns = ["conta_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SubcategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoria_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("cartao_id"),
        Index("conta_id"),
        Index("categoria_id"),
        Index("subcategoria_id")
    ]
)
data class TransacaoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "valor")
    val valor: Double,

    @ColumnInfo(name = "data_competencia")
    val dataCompetencia: Long, // epoch millis

    @ColumnInfo(name = "tipo")
    val tipo: String, // "receita" | "despesa"

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long?,

    @ColumnInfo(name = "subcategoria_id")
    val subcategoriaId: Long?,

    @ColumnInfo(name = "forma_pagamento")
    val formaPagamento: String, // "cartao" | "conta" | "dinheiro"

    @ColumnInfo(name = "cartao_id")
    val cartaoId: Long?,

    @ColumnInfo(name = "conta_id")
    val contaId: Long?,

    @ColumnInfo(name = "parcelado")
    val parcelado: Boolean = false,

    @ColumnInfo(name = "numero_parcelas")
    val numeroParcelas: Int = 1,

    @ColumnInfo(name = "parcela_atual")
    val parcelaAtual: Int = 1,

    @ColumnInfo(name = "observacao")
    val observacao: String? = null,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)