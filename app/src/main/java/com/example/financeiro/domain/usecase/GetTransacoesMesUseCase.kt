package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.TransacaoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransacoesMesUseCase @Inject constructor(
    private val transacaoRepository: TransacaoRepository
) {
    operator fun invoke(inicioMes: Long, fimMes: Long): Flow<List<Transacao>> =
        transacaoRepository.getByMes(inicioMes, fimMes)
}