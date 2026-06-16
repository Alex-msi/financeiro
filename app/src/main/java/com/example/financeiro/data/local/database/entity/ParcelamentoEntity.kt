package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parcelamentos",
    foreignKeys = [
        ForeignKey(
            entity = TransacaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["transacao_principal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CartaoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cartao_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("transacao_principal_id"),
        Index("cartao_id")
    ]
)
data class ParcelamentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "transacao_principal_id")
    val transacaoPrincipalId: Long,

    @ColumnInfo(name = "valor_parcela")
    val valorParcela: Double,

    @ColumnInfo(name = "total_parcelas")
    val totalParcelas: Int,

    @ColumnInfo(name = "parcelas_pagas")
    val parcelasPagas: Int = 0,

    @ColumnInfo(name = "data_primeira_parcela")
    val dataPrimeiraParcela: Long, // epoch millis

    @ColumnInfo(name = "cartao_id")
    val cartaoId: Long?,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)