package com.example.financeiro.ui.extrato

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ItemExtratoUi(
    val id: Long,
    val descricao: String,
    val categoria: String,
    val data: String,
    val valor: Double,
    val valorFormatado: String,
    val isCredito: Boolean
)

data class ExtratoContaUiState(
    val isLoading: Boolean = true,
    val nomeConta: String = "",
    val mes: Int = Calendar.getInstance().get(Calendar.MONTH),
    val ano: Int = Calendar.getInstance().get(Calendar.YEAR),
    val saldoAnterior: Double = 0.0,
    val creditos: Double = 0.0,
    val debitos: Double = 0.0,
    val saldoFinal: Double = 0.0,
    val itens: List<ItemExtratoUi> = emptyList()
) {
    val labelMes: String get() = "${nomeMes(mes)} $ano"
    val saldoAnteriorFormatado: String get() = formatar(saldoAnterior)
    val creditosFormatados: String get() = formatar(creditos)
    val debitosFormatados: String get() = formatar(debitos)
    val saldoFinalFormatado: String get() = formatar(saldoFinal)
    val isEmpty: Boolean get() = !isLoading && itens.isEmpty()

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]

        fun formatar(valor: Double): String = "R$ %,.2f".format(valor)
    }
}

@HiltViewModel
class ExtratoContaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contaRepository: ContaRepository,
    transacaoRepository: TransacaoRepository,
    categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val contaId: Long = checkNotNull(savedStateHandle["contaId"])
    private val mesSelecionado = MutableStateFlow(mesAnoAtual())

    val uiState: StateFlow<ExtratoContaUiState> = combine(
        contaRepository.getAll(),
        transacaoRepository.getAll(),
        categoriaRepository.getAll(),
        mesSelecionado
    ) { contas, transacoes, categorias, (mes, ano) ->
        val conta = contas.firstOrNull { it.id == contaId }
            ?: return@combine ExtratoContaUiState(isLoading = false, mes = mes, ano = ano)
        val (inicio, fim) = intervaloMes(mes, ano)
        val movimentosConta = transacoes.filter {
            it.formaPagamento == "conta" && it.contaId == contaId
        }
        val saldoAnterior = conta.saldoAtual + movimentosConta
            .filter { it.dataCompetencia < inicio }
            .sumOf { it.valorAssinado() }
        val movimentosMes = movimentosConta
            .filter { it.dataCompetencia >= inicio && it.dataCompetencia < fim }
            .sortedByDescending { it.dataCompetencia }
        val creditos = movimentosMes.filter { it.tipo == "receita" }.sumOf { it.valor }
        val debitos = movimentosMes.filter { it.tipo == "despesa" }.sumOf { it.valor }
        val categoriasMap = categorias.associate { it.id to it.nome }

        ExtratoContaUiState(
            isLoading = false,
            nomeConta = conta.nome,
            mes = mes,
            ano = ano,
            saldoAnterior = saldoAnterior,
            creditos = creditos,
            debitos = debitos,
            saldoFinal = saldoAnterior + creditos - debitos,
            itens = movimentosMes.map {
                it.toUi(it.categoriaId?.let(categoriasMap::get) ?: "Sem categoria")
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExtratoContaUiState()
    )

    fun irParaMesAnterior() {
        mesSelecionado.update { (mes, ano) ->
            if (mes == 0) 11 to ano - 1 else mes - 1 to ano
        }
    }

    fun irParaProximoMes() {
        mesSelecionado.update { (mes, ano) ->
            if (mes == 11) 0 to ano + 1 else mes + 1 to ano
        }
    }

    private fun Transacao.toUi(categoriaNome: String): ItemExtratoUi =
        ItemExtratoUi(
            id = id,
            descricao = RegrasFinanceiras.descricaoVisivel(this)?.takeIf { it.isNotBlank() }
                ?: if (tipo == "receita") "Crédito na conta" else "Débito na conta",
            categoria = categoriaNome,
            data = formatarData(dataCompetencia),
            valor = valor,
            valorFormatado = ExtratoContaUiState.formatar(valor),
            isCredito = tipo == "receita"
        )

    private fun Transacao.valorAssinado(): Double =
        if (tipo == "receita") valor else -valor

    private fun intervaloMes(mes: Int, ano: Int): Pair<Long, Long> {
        val inicio = Calendar.getInstance().apply {
            set(ano, mes, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fim = Calendar.getInstance().apply {
            timeInMillis = inicio
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return inicio to fim
    }

    private fun formatarData(valor: Long): String {
        val data = Calendar.getInstance().apply { timeInMillis = valor }
        return "%02d/%02d/%04d".format(
            data.get(Calendar.DAY_OF_MONTH),
            data.get(Calendar.MONTH) + 1,
            data.get(Calendar.YEAR)
        )
    }

    private fun mesAnoAtual(): Pair<Int, Int> {
        val agora = Calendar.getInstance()
        return agora.get(Calendar.MONTH) to agora.get(Calendar.YEAR)
    }
}
