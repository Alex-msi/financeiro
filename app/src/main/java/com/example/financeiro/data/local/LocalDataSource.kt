package com.example.financeiro.data.local

import com.example.financeiro.data.local.database.dao.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDataSource @Inject constructor(
    val contaDao: ContaDao,
    val cartaoDao: CartaoDao,
    val categoriaDao: CategoriaDao,
    val subcategoriaDao: SubcategoriaDao,
    val transacaoDao: TransacaoDao,
    val parcelamentoDao: ParcelamentoDao
)