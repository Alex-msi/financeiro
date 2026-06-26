package com.example.financeiro.domain.usecase

import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class GerarParcelasUseCase @Inject constructor(
    private val cartaoRepository: CartaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository
) {
    suspend operator fun invoke(transacao: Transacao): Result<Unit> = runCatching {
        require(transacao.cartaoId != null) { "Parcelamento exige cartão" }
        require(transacao.numeroParcelas > 1) { "Parcelamento exige mais de 1 parcela" }

        val cartao = cartaoRepository.getAllAtivos().first()
            .find { it.id == transacao.cartaoId }
            ?: error("Cartão não encontrado")

        val valorParcela = transacao.valor / transacao.numeroParcelas

        // Determina o mês da primeira fatura
        val dataCompra = Calendar.getInstance().apply {
            timeInMillis = transacao.dataCompetencia
        }
        val diaCompra = dataCompra.get(Calendar.DAY_OF_MONTH)

        val mesFechamentoOffset = if (diaCompra > cartao.diaFechamento) 1 else 0
        val mesVencimentoOffset = if (cartao.diaVencimento <= cartao.diaFechamento) 1 else 0

        val dataPrimeiraParcela = Calendar.getInstance().apply {
            timeInMillis = transacao.dataCompetencia
            add(Calendar.MONTH, mesFechamentoOffset + mesVencimentoOffset)
            set(Calendar.DAY_OF_MONTH, cartao.diaVencimento)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        parcelamentoRepository.insert(
            Parcelamento(
                transacaoPrincipalId = transacao.id,
                valorParcela = valorParcela,
                totalParcelas = transacao.numeroParcelas,
                parcelasPagas = 0,
                dataPrimeiraParcela = dataPrimeiraParcela,
                cartaoId = transacao.cartaoId,
                criadoEm = System.currentTimeMillis()
            )
        )
    }
}
