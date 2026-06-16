package com.example.financeiro.data.local.mapper

import com.example.financeiro.data.local.database.entity.CategoriaEntity
import com.example.financeiro.data.local.database.entity.SubcategoriaEntity
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Subcategoria

fun CategoriaEntity.toDomain() = Categoria(
    id = id,
    nome = nome,
    tipo = tipo,
    cor = cor,
    criadoEm = criadoEm
)

fun Categoria.toEntity() = CategoriaEntity(
    id = id,
    nome = nome,
    tipo = tipo,
    cor = cor,
    criadoEm = criadoEm
)

fun SubcategoriaEntity.toDomain() = Subcategoria(
    id = id,
    nome = nome,
    categoriaId = categoriaId,
    ativa = ativa,
    criadoEm = criadoEm
)

fun Subcategoria.toEntity() = SubcategoriaEntity(
    id = id,
    nome = nome,
    categoriaId = categoriaId,
    ativa = ativa,
    criadoEm = criadoEm
)