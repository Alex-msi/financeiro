package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.repository.ContaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSaldoTenhoUseCase @Inject constructor(
    private val contaRepository: ContaRepository
) {
    operator fun invoke(): Flow<Double> =
        contaRepository.getSaldoTotal().map { it ?: 0.0 }
}