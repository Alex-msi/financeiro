package com.example.financeiro.data.local.mapper

import com.example.financeiro.data.local.database.entity.TransacaoEntity
import com.example.financeiro.domain.model.Transacao

fun TransacaoEntity.toDomain() = Transacao(
    id = id,
    valor = valor,
    dataCompetencia = dataCompetencia,
    tipo = tipo,
    categoriaId = categoriaId,
    subcategoriaId = subcategoriaId,
    formaPagamento = formaPagamento,
    cartaoId = cartaoId,
    contaId = contaId,
    parcelado = parcelado,
    numeroParcelas = numeroParcelas,
    parcelaAtual = parcelaAtual,
    observacao = observacao,
    recorrenciaId = recorrenciaId,
    recorrenciaIndice = recorrenciaIndice,
    criadoEm = criadoEm
)

fun Transacao.toEntity() = TransacaoEntity(
    id = id,
    valor = valor,
    dataCompetencia = dataCompetencia,
    tipo = tipo,
    categoriaId = categoriaId,
    subcategoriaId = subcategoriaId,
    formaPagamento = formaPagamento,
    cartaoId = cartaoId,
    contaId = contaId,
    parcelado = parcelado,
    numeroParcelas = numeroParcelas,
    parcelaAtual = parcelaAtual,
    observacao = observacao,
    recorrenciaId = recorrenciaId,
    recorrenciaIndice = recorrenciaIndice,
    criadoEm = criadoEm
)
