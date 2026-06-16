package com.example.financeiro.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategorias",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoria_id")]
)
data class SubcategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    @ColumnInfo(name = "categoria_id")
    val categoriaId: Long,

    val ativa: Boolean = true,

    @ColumnInfo(name = "criado_em")
    val criadoEm: Long = System.currentTimeMillis()
)