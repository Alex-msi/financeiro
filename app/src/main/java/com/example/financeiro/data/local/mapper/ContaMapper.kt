package com.example.financeiro.data.local.mapper

import com.example.financeiro.data.local.database.entity.ContaEntity
import com.example.financeiro.domain.model.Conta

fun ContaEntity.toDomain() = Conta(
    id = id,
    nome = nome,
    tipo = tipo,
    saldoInicial = saldoInicial,
    saldoAtual = saldoAtual,
    ativa = ativa,
    criadoEm = criadoEm
)

fun Conta.toEntity() = ContaEntity(
    id = id,
    nome = nome,
    tipo = tipo,
    saldoInicial = saldoInicial,
    saldoAtual = saldoAtual,
    ativa = ativa,
    criadoEm = criadoEm
)