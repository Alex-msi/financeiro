package com.example.financeiro.ui.transacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import com.example.financeiro.domain.usecase.GetTransacoesMesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ─── UiState ─────────────────────────────────────────────────────────────────

data class TransacaoListUiState(
    val isLoading: Boolean = true,
    val transacoes: List<TransacaoItemUi> = emptyList(),
    val mesAtual: Int = Calendar.getInstance().get(Calendar.MONTH),
    val anoAtual: Int = Calendar.getInstance().get(Calendar.YEAR),
    val erro: String? = null
) {
    val labelMes: String get() = "${nomeMes(mesAtual)} $anoAtual"
    val isEmpty: Boolean get() = !isLoading && transacoes.isEmpty()

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

/**
 * Modelo de UI de cada linha da lista — já com strings formatadas
 * para evitar lógica de formatação no Adapter.
 */
data class TransacaoItemUi(
    val id: Long,
    val descricao: String,           // observacao ou fallback "Sem descrição"
    val valorFormatado: String,      // "R$ 150,00"
    val dataFormatada: String,       // "15/04/2025"
    val categoriaNome: String,       // nome da categoria ou "Sem categoria"
    val tipo: String,                // "receita" ou "despesa"
    val isReceita: Boolean
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class TransacaoListViewModel @Inject constructor(
    private val getTransacoesMesUseCase: GetTransacoesMesUseCase,
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _mesSelecionado = MutableStateFlow(mesAnoAtual())

    // Mapa categoriaId → nome, atualizado reativamente
    private val _categoriasMap = MutableStateFlow<Map<Long, String>>(emptyMap())

    init {
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            categoriaRepository.getAll().collect { lista ->
                _categoriasMap.value = lista.associate { it.id to it.nome }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransacaoListUiState> = combine(
        _mesSelecionado.flatMapLatest { (mes, ano) ->
            val (inicio, fim) = intervaloMes(mes, ano)
            getTransacoesMesUseCase(inicio, fim)
        },
        _mesSelecionado,
        _categoriasMap
    ) { transacoes, (mes, ano), categoriasMap ->
        TransacaoListUiState(
            isLoading = false,
            transacoes = transacoes
                .sortedByDescending { it.dataCompetencia }
                .map { it.toUi(categoriasMap) },
            mesAtual = mes,
            anoAtual = ano
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransacaoListUiState(isLoading = true)
    )

    // ─── Navegação entre meses ────────────────────────────────────────────────

    fun irParaMesAnterior() {
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 0) Pair(11, ano - 1) else Pair(mes - 1, ano)
        }
    }

    fun irParaProximoMes() {
        if (!uiState.value.podeIrParaProximoMes) return
        _mesSelecionado.update { (mes, ano) ->
            if (mes == 11) Pair(0, ano + 1) else Pair(mes + 1, ano)
        }
    }

    // ─── Ações ────────────────────────────────────────────────────────────────

    fun deletarTransacao(id: Long) {
        viewModelScope.launch {
            try {
                val transacao = transacaoRepository.getById(id) ?: return@launch
                transacaoRepository.delete(transacao)
            } catch (e: Exception) {
                // erro silencioso — o item volta para a lista via Flow reativo
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun mesAnoAtual(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    private fun intervaloMes(mes: Int, ano: Int): Pair<Long, Long> {
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

    private fun Transacao.toUi(categoriasMap: Map<Long, String>): TransacaoItemUi {
        val cal = Calendar.getInstance().apply { timeInMillis = dataCompetencia }
        val dataFormatada = "%02d/%02d/%04d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        return TransacaoItemUi(
            id = id,
            descricao = if (!observacao.isNullOrBlank()) observacao else "Sem descrição",
            valorFormatado = "R$ %,.2f".format(valor),
            dataFormatada = dataFormatada,
            categoriaNome = categoriaId?.let { categoriasMap[it] } ?: "Sem categoria",
            tipo = tipo,
            isReceita = tipo == "receita"
        )
    }
}