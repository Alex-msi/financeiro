package com.example.financeiro.data.local.mapper

import com.example.financeiro.data.local.database.entity.CartaoEntity
import com.example.financeiro.domain.model.Cartao

fun CartaoEntity.toDomain() = Cartao(
    id = id,
    nome = nome,
    limiteTotal = limiteTotal,
    //limiteDisponivel = limiteTotal, // entity não tem esse campo, usa limiteTotal como fallback
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    ativo = ativo,
    criadoEm = criadoEm
)

fun Cartao.toEntity() = CartaoEntity(
    id = id,
    nome = nome,
    limiteTotal = limiteTotal,
    diaFechamento = diaFechamento,
    diaVencimento = diaVencimento,
    ativo = ativo,
    criadoEm = criadoEm
)