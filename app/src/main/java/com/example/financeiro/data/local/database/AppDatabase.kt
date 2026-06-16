package com.example.financeiro.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.financeiro.data.local.database.dao.CartaoDao
import com.example.financeiro.data.local.database.dao.CategoriaDao
import com.example.financeiro.data.local.database.dao.ContaDao
import com.example.financeiro.data.local.database.dao.ParcelamentoDao
import com.example.financeiro.data.local.database.dao.SubcategoriaDao
import com.example.financeiro.data.local.database.dao.TransacaoDao
import com.example.financeiro.data.local.database.entity.CartaoEntity
import com.example.financeiro.data.local.database.entity.CategoriaEntity
import com.example.financeiro.data.local.database.entity.ContaEntity
import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import com.example.financeiro.data.local.database.entity.SubcategoriaEntity
import com.example.financeiro.data.local.database.entity.TransacaoEntity

@Database(
    entities = [
        ContaEntity::class,
        CartaoEntity::class,
        CategoriaEntity::class,
        SubcategoriaEntity::class,
        TransacaoEntity::class,
        ParcelamentoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contaDao(): ContaDao
    abstract fun cartaoDao(): CartaoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun subcategoriaDao(): SubcategoriaDao
    abstract fun transacaoDao(): TransacaoDao
    abstract fun parcelamentoDao(): ParcelamentoDao
}