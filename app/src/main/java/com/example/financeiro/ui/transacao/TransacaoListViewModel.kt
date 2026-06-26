package com.example.financeiro.ui.transacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class TransacaoListUiState(
    val isLoading: Boolean = true,
    val transacoes: List<TransacaoItemUi> = emptyList(),
    val filtroAtual: FiltroTransacao = FiltroTransacao.TODOS,
    val mesAtual: Int = Calendar.getInstance().get(Calendar.MONTH),
    val anoAtual: Int = Calendar.getInstance().get(Calendar.YEAR),
    val erro: String? = null
) {
    val labelMes: String get() = "${nomeMes(mesAtual)} $anoAtual"
    val isEmpty: Boolean get() = !isLoading && transacoes.isEmpty()

    val podeIrParaProximoMes: Boolean get() = true

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]
    }
}

enum class FiltroTransacao(val formaPagamento: String?) {
    TODOS(null),
    CONTA("conta"),
    CARTAO("cartao"),
    DINHEIRO("dinheiro")
}

data class TransacaoItemUi(
    val itemKey: String,
    val id: Long,
    val descricao: String,
    val valorFormatado: String,
    val dataFormatada: String,
    val categoriaNome: String,
    val tipo: String,
    val formaPagamento: String,
    val isReceita: Boolean,
    val parcelamentoInfo: String? = null,
    val ordenacao: Long = 0L
)

@HiltViewModel
class TransacaoListViewModel @Inject constructor(
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _mesSelecionado = MutableStateFlow(mesAnoAtual())
    private val _filtroSelecionado = MutableStateFlow(FiltroTransacao.TODOS)
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
        transacaoRepository.getAll(),
        _mesSelecionado,
        _filtroSelecionado,
        _categoriasMap,
        parcelamentoRepository.getAll()
    ) { transacoes, (mes, ano), filtro, categoriasMap, parcelamentos ->
        val (inicio, fim) = intervaloMes(mes, ano)
        val parcelamentosPorTransacao = parcelamentos.associateBy { it.transacaoPrincipalId }
        val transacoesDoMes = transacoes
            .filter { it.dataCompetencia >= inicio && it.dataCompetencia < fim }
            .map { transacao -> transacao.toUi(categoriasMap, parcelamentosPorTransacao[transacao.id]) }
        val transacoesPorId = transacoes.associateBy { it.id }
        val parcelasDoMes = parcelamentos.flatMap { parcelamento ->
            val transacao = transacoesPorId[parcelamento.transacaoPrincipalId] ?: return@flatMap emptyList()
            parcelamento.toParcelasUi(transacao, categoriasMap, inicio, fim)
        }

        TransacaoListUiState(
            isLoading = false,
            transacoes = (transacoesDoMes + parcelasDoMes)
                .filter { item -> filtro.formaPagamento == null || item.formaPagamento == filtro.formaPagamento }
                .sortedByDescending { it.ordenacao },
            filtroAtual = filtro,
            mesAtual = mes,
            anoAtual = ano
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransacaoListUiState(isLoading = true)
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

    fun definirMes(mes: Int, ano: Int) {
        if (mes !in 0..11 || ano <= 0) return
        _mesSelecionado.value = Pair(mes, ano)
    }

    fun alterarFiltro(filtro: FiltroTransacao) {
        _filtroSelecionado.value = filtro
    }

    fun deletarTransacao(id: Long) {
        viewModelScope.launch {
            runCatching {
                val transacao = transacaoRepository.getById(id) ?: return@launch
                transacaoRepository.delete(transacao)
            }
        }
    }

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

    private fun Transacao.toUi(
        categoriasMap: Map<Long, String>,
        parcelamento: Parcelamento?
    ): TransacaoItemUi {
        val cal = Calendar.getInstance().apply { timeInMillis = dataCompetencia }
        val dataFormatada = "%02d/%02d/%04d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        return TransacaoItemUi(
            itemKey = "transacao-$id",
            id = id,
            descricao = RegrasFinanceiras.descricaoVisivel(this)?.takeIf { it.isNotBlank() } ?: "Sem descrição",
            valorFormatado = "R$ %,.2f".format(valor),
            dataFormatada = dataFormatada,
            categoriaNome = "${formaPagamento.toLabel()} - ${categoriaId?.let { categoriasMap[it] } ?: "Sem categoria"}",
            tipo = tipo,
            formaPagamento = formaPagamento,
            isReceita = tipo == "receita",
            parcelamentoInfo = parcelamento?.toLancamentoInfo(valor),
            ordenacao = dataCompetencia
        )
    }

    private fun Parcelamento.toParcelasUi(
        transacao: Transacao,
        categoriasMap: Map<Long, String>,
        inicioMes: Long,
        fimMes: Long
    ): List<TransacaoItemUi> {
        return (parcelasPagas until totalParcelas).mapNotNull { indice ->
            val vencimento = dataParcela(indice)
            if (vencimento < inicioMes || vencimento >= fimMes) return@mapNotNull null

            val parcelaAtual = indice + 1
            val pendentes = (totalParcelas - parcelasPagas).coerceAtLeast(0)
            TransacaoItemUi(
                itemKey = "parcela-${transacao.id}-$indice",
                id = transacao.id,
                descricao = "${RegrasFinanceiras.descricaoVisivel(transacao) ?: "Compra parcelada"} ($parcelaAtual/$totalParcelas)",
                valorFormatado = formatarValor(valorParcela),
                dataFormatada = dataParaString(vencimento),
                categoriaNome = "Cartão - ${transacao.categoriaId?.let { categoriasMap[it] } ?: "Sem categoria"}",
                tipo = "despesa",
                formaPagamento = "cartao",
                isReceita = false,
                parcelamentoInfo = "Parcela: ${formatarValor(valorParcela)} - $pendentes pendente(s)",
                ordenacao = vencimento
            )
        }
    }

    private fun Parcelamento.dataParcela(indice: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dataPrimeiraParcela
            add(Calendar.MONTH, indice)
        }.timeInMillis

    private fun Parcelamento.toInfo(): String {
        val pendentes = (totalParcelas - parcelasPagas).coerceAtLeast(0)
        return "Parcelado: ${totalParcelas}x de ${formatarValor(valorParcela)} - $pendentes pendente(s)"
    }

    private fun Parcelamento.toLancamentoInfo(valorTotal: Double): String {
        val pendentes = (totalParcelas - parcelasPagas).coerceAtLeast(0)
        return "Lancamento parcelado: ${formatarValor(valorTotal)} em ${totalParcelas}x de ${formatarValor(valorParcela)} - $pendentes pendente(s)"
    }

    private fun formatarValor(valor: Double): String =
        "R$ %,.2f".format(valor)

    private fun String.toLabel(): String =
        when (this) {
            "conta" -> "Conta"
            "cartao" -> "Cartão"
            "dinheiro" -> "Dinheiro"
            else -> this
        }

    private fun dataParaString(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return "%02d/%02d/%04d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
    }
}
