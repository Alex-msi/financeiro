package com.example.financeiro.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.usecase.GetResumoMesUseCase
import com.example.financeiro.domain.usecase.GetSaldoTenhoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.Calendar
import javax.inject.Inject

// ─── UiState ─────────────────────────────────────────────────────────────────

data class DashboardUiState(
    val isLoading: Boolean = true,
    val saldoTenho: Double = 0.0,
    val totalReceitas: Double = 0.0,
    val totalDespesas: Double = 0.0,
    val saldoMes: Double = 0.0,
    val mesAtual: Int = Calendar.getInstance().get(Calendar.MONTH),      // 0-11
    val anoAtual: Int = Calendar.getInstance().get(Calendar.YEAR),
    val erro: String? = null
) {
    /** Rótulo formatado para exibir no header, ex: "Abril 2025" */
    val labelMes: String get() = "${nomeMes(mesAtual)} $anoAtual"

    val podeirParaMesAnterior: Boolean get() = true  // sem limite de navegação
    val podeIrParaProximoMes: Boolean get() {
        val agora = Calendar.getInstance()
        return anoAtual < agora.get(Calendar.YEAR) ||
                (anoAtual == agora.get(Calendar.YEAR) && mesAtual < agora.get(Calendar.MONTH))
    }

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]
    }
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSaldoTenhoUseCase: GetSaldoTenhoUseCase,
    private val getResumoMesUseCase: GetResumoMesUseCase
) : ViewModel() {

    // Mês/ano selecionado pelo usuário (estado de navegação)
    private val _mesSelecionado = MutableStateFlow(mesAnoAtual())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        getSaldoTenhoUseCase(),
        _mesSelecionado.flatMapLatest { (mes, ano) ->
            val (inicio, fim) = intervaloMes(mes, ano)
            getResumoMesUseCase(inicio, fim)
        },
        _mesSelecionado
    ) { saldo, resumo, (mes, ano) ->
        DashboardUiState(
            isLoading = false,
            saldoTenho = saldo,
            totalReceitas = resumo.totalReceitas,
            totalDespesas = resumo.totalDespesas,
            saldoMes = resumo.saldo,
            mesAtual = mes,
            anoAtual = ano
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    // ─── Navegação entre meses ────────────────────────────────────────────────

    fun irParaMesAnterior() {
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 0) Pair(11, ano - 1) else Pair(mes - 1, ano)
        }
    }

    fun irParaProximoMes() {
        val state = uiState.value
        if (!state.podeIrParaProximoMes) return
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 11) Pair(0, ano + 1) else Pair(mes + 1, ano)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun mesAnoAtual(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    /**
     * Retorna o epoch millis do primeiro e último instante do mês informado.
     * ex: mes=3 (Abril), ano=2025 → 1º de Abril 00:00:00 até 30 de Abril 23:59:59
     */
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
}