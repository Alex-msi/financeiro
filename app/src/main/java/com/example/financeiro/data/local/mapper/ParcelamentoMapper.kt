package com.example.financeiro.data.local.mapper

import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import com.example.financeiro.domain.model.Parcelamento

fun ParcelamentoEntity.toDomain() = Parcelamento(
    id = id,
    transacaoPrincipalId = transacaoPrincipalId,
    valorParcela = valorParcela,
    totalParcelas = totalParcelas,
    parcelasPagas = parcelasPagas,
    dataPrimeiraParcela = dataPrimeiraParcela,
    cartaoId = cartaoId,
    criadoEm = criadoEm
)

fun Parcelamento.toEntity() = ParcelamentoEntity(
    id = id,
    transacaoPrincipalId = transacaoPrincipalId,
    valorParcela = valorParcela,
    totalParcelas = totalParcelas,
    parcelasPagas = parcelasPagas,
    dataPrimeiraParcela = dataPrimeiraParcela,
    cartaoId = cartaoId,
    criadoEm = criadoEm
)