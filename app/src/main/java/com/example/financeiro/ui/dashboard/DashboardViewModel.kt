package com.example.financeiro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import com.example.financeiro.domain.usecase.GetSaldoAtualUseCase
import com.example.financeiro.domain.usecase.GetResumoMesUseCase
import com.example.financeiro.domain.usecase.GetSaldoTenhoUseCase
import com.example.financeiro.domain.usecase.PagarFaturaCartaoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val saldoTenho: Double = 0.0,
    val saldoAtual: Double = 0.0,
    val totalReceitas: Double = 0.0,
    val totalDespesas: Double = 0.0,
    val saldoMes: Double = 0.0,
    val faturas: List<FaturaCartaoUi> = emptyList(),
    val contasPagamento: List<ContaPagamentoUi> = emptyList(),
    val mesAtual: Int = Calendar.getInstance().get(Calendar.MONTH),
    val anoAtual: Int = Calendar.getInstance().get(Calendar.YEAR),
    val erro: String? = null
) {
    val labelMes: String get() = "${nomeMes(mesAtual)} $anoAtual"

    val podeirParaMesAnterior: Boolean get() = true
    val podeIrParaProximoMes: Boolean get() = true

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]
    }
}

data class FaturaCartaoUi(
    val cartaoId: Long,
    val nomeCartao: String,
    val valor: Double,
    val valorFormatado: String,
    val quantidadeParcelas: Int,
    val resumoParcelas: String
)

data class ContaPagamentoUi(
    val id: Long,
    val nome: String
)

sealed class DashboardEvento {
    data class FaturaPaga(val cartaoId: Long, val valor: Double) : DashboardEvento()
    data class Erro(val mensagem: String) : DashboardEvento()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSaldoTenhoUseCase: GetSaldoTenhoUseCase,
    private val getSaldoAtualUseCase: GetSaldoAtualUseCase,
    private val getResumoMesUseCase: GetResumoMesUseCase,
    private val cartaoRepository: CartaoRepository,
    private val contaRepository: ContaRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val transacaoRepository: TransacaoRepository,
    private val pagarFaturaCartaoUseCase: PagarFaturaCartaoUseCase
) : ViewModel() {

    private val _mesSelecionado = MutableStateFlow(mesAnoAtual())
    private val _eventos = MutableSharedFlow<DashboardEvento>()
    val eventos: SharedFlow<DashboardEvento> = _eventos.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _mesSelecionado.flatMapLatest { (mes, ano) ->
            val referenciaSaldo = referenciaSaldo(mes, ano)
            combine(
                getSaldoTenhoUseCase(referenciaSaldo),
                getSaldoAtualUseCase(referenciaSaldo)
            ) { saldoTenho, saldoAtual -> saldoTenho to saldoAtual }
        },
        _mesSelecionado.flatMapLatest { (mes, ano) ->
            val (inicio, fim) = intervaloMes(mes, ano)
            getResumoMesUseCase(inicio, fim)
        },
        combine(
            cartaoRepository.getAll(),
            transacaoRepository.getAll(),
            contaRepository.getAllAtivas()
        ) { cartoes, transacoes, contas -> Triple(cartoes, transacoes, contas) },
        parcelamentoRepository.getParcelamentosEmAberto(),
        _mesSelecionado
    ) { saldos, resumo, cartoesTransacoesContas, parcelamentos, (mes, ano) ->
        val (saldoTenho, saldoAtual) = saldos
        val cartoes = cartoesTransacoesContas.first
        val transacoes = cartoesTransacoesContas.second
        val contas = cartoesTransacoesContas.third
        val (inicio, fim) = intervaloMes(mes, ano)
        DashboardUiState(
            isLoading = false,
            saldoTenho = saldoTenho,
            saldoAtual = saldoAtual,
            totalReceitas = resumo.totalReceitas,
            totalDespesas = resumo.totalDespesas,
            saldoMes = resumo.saldo,
            faturas = cartoes.mapNotNull { cartao ->
                val calculoFatura = calcularFatura(
                    cartaoId = cartao.id,
                    inicioMes = inicio,
                    fimMes = fim,
                    parcelamentos = parcelamentos,
                    transacoes = transacoes,
                    dataFaturaCompra = { compra -> RegrasFinanceiras.dataFatura(compra, cartao) }
                )
                if (calculoFatura.quantidadeParcelas == 0) return@mapNotNull null
                FaturaCartaoUi(
                    cartaoId = cartao.id,
                    nomeCartao = cartao.nome,
                    valor = calculoFatura.valor,
                    valorFormatado = formatarValor(calculoFatura.valor),
                    quantidadeParcelas = calculoFatura.quantidadeParcelas,
                    resumoParcelas = resumoParcelas(calculoFatura.quantidadeParcelas)
                )
            },
            contasPagamento = contas.map { ContaPagamentoUi(it.id, it.nome) },
            mesAtual = mes,
            anoAtual = ano
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun irParaMesAnterior() {
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 0) Pair(11, ano - 1) else Pair(mes - 1, ano)
        }
    }

    fun irParaProximoMes() {
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 11) Pair(0, ano + 1) else Pair(mes + 1, ano)
        }
    }

    fun pagarFatura(cartaoId: Long, formaPagamento: String, contaId: Long?, valorPagamento: Double) {
        viewModelScope.launch {
            val state = uiState.value
            val (inicio, fim) = intervaloMes(state.mesAtual, state.anoAtual)
            pagarFaturaCartaoUseCase(cartaoId, inicio, fim, formaPagamento, contaId, valorPagamento)
                .onSuccess { valor -> _eventos.emit(DashboardEvento.FaturaPaga(cartaoId, valor)) }
                .onFailure { erro ->
                    _eventos.emit(
                        DashboardEvento.Erro(erro.message ?: "Não foi possível pagar a fatura")
                    )
                }
        }
    }

    private fun mesAnoAtual(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun intervaloMes(mes: Int, ano: Int): Pair<Long, Long> {
        val inicio = Calendar.getInstance().apply {
            set(ano, mes, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val fim = Calendar.getInstance().apply {
            set(ano, mes, 1, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }.timeInMillis

        return Pair(inicio, fim)
    }

    private fun referenciaSaldo(mes: Int, ano: Int): Long {
        val agora = Calendar.getInstance()
        val mesAtual = agora.get(Calendar.MONTH)
        val anoAtual = agora.get(Calendar.YEAR)
        return if (mes == mesAtual && ano == anoAtual) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        } else {
            intervaloMes(mes, ano).second
        }
    }

    private data class CalculoFatura(
        val valor: Double,
        val quantidadeParcelas: Int
    )

    private fun calcularFatura(
        cartaoId: Long,
        inicioMes: Long,
        fimMes: Long,
        parcelamentos: List<Parcelamento>,
        transacoes: List<Transacao>,
        dataFaturaCompra: (Transacao) -> Long
    ): CalculoFatura {
        val parcelas = parcelamentos
            .filter { it.cartaoId == cartaoId }
            .fold(CalculoFatura(0.0, 0)) { acumulado, parcelamento ->
            val indice = RegrasFinanceiras.indiceParcelaNoMes(parcelamento, inicioMes)
            val entraNaFatura = indice == parcelamento.parcelasPagas &&
                indice in 0 until parcelamento.totalParcelas &&
                RegrasFinanceiras.estaNoPeriodo(
                    RegrasFinanceiras.dataParcela(parcelamento, indice),
                    inicioMes,
                    fimMes
                )
            if (!entraNaFatura) return@fold acumulado

            CalculoFatura(
                valor = acumulado.valor + parcelamento.valorParcela,
                quantidadeParcelas = acumulado.quantidadeParcelas + 1
            )
        }
        val comprasAvulsas = transacoes.filter {
            it.tipo == "despesa" &&
                it.formaPagamento == "cartao" &&
                it.cartaoId == cartaoId &&
                !RegrasFinanceiras.isPagamentoFatura(it) &&
                !it.parcelado &&
                RegrasFinanceiras.estaNoPeriodo(dataFaturaCompra(it), inicioMes, fimMes)
        }
        val totalFatura = parcelas.valor + comprasAvulsas.sumOf { it.valor }
        val totalPago = RegrasFinanceiras.totalPagoFatura(transacoes, cartaoId, inicioMes)
        val aberto = (totalFatura - totalPago).coerceAtLeast(0.0)
        return CalculoFatura(
            valor = aberto,
            quantidadeParcelas = if (aberto > 0.009) parcelas.quantidadeParcelas + comprasAvulsas.size else 0
        )
    }

    private fun resumoParcelas(quantidade: Int): String =
        if (quantidade == 1) "1 parcela aberta neste mês" else "$quantidade parcelas abertas neste mês"

    private fun formatarValor(valor: Double): String =
        "R$ %,.2f".format(valor)
}
