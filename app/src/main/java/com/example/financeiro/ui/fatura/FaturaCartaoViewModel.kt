package com.example.financeiro.ui.fatura

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import com.example.financeiro.domain.usecase.PagarFaturaCartaoUseCase
import com.example.financeiro.ui.dashboard.ContaPagamentoUi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemFaturaUi(
    val chave: String,
    val descricao: String,
    val categoria: String,
    val data: String,
    val dataMillis: Long,
    val valor: Double,
    val valorFormatado: String,
    val detalhe: String,
    val pago: Boolean,
    val pagavel: Boolean,
    val pagamento: Boolean = false
)

data class FaturaCartaoUiState(
    val isLoading: Boolean = true,
    val nomeCartao: String = "",
    val mes: Int = Calendar.getInstance().get(Calendar.MONTH),
    val ano: Int = Calendar.getInstance().get(Calendar.YEAR),
    val total: Double = 0.0,
    val totalPago: Double = 0.0,
    val totalAberto: Double = 0.0,
    val faturaPaga: Boolean = false,
    val contasPagamento: List<ContaPagamentoUi> = emptyList(),
    val itens: List<ItemFaturaUi> = emptyList()
) {
    val labelMes: String get() = "${nomeMes(mes)} $ano"
    val totalFormatado: String get() = "R$ %,.2f".format(total)
    val totalPagoFormatado: String get() = "R$ %,.2f".format(totalPago)
    val totalAbertoFormatado: String get() = "R$ %,.2f".format(totalAberto)
    val isEmpty: Boolean get() = !isLoading && itens.isEmpty()
    val podePagar: Boolean get() = !isLoading && !faturaPaga && totalAberto > 0.0
    val periodo: String
        get() {
            val agora = Calendar.getInstance()
            val atual = agora.get(Calendar.YEAR) * 12 + agora.get(Calendar.MONTH)
            val selecionado = ano * 12 + mes
            return when {
                selecionado < atual -> "Fatura passada"
                selecionado > atual -> "Fatura futura"
                else -> "Fatura atual"
            }
        }

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]
    }
}

sealed class FaturaCartaoEvento {
    data class FaturaPaga(val valor: Double) : FaturaCartaoEvento()
    data class Erro(val mensagem: String) : FaturaCartaoEvento()
}

@HiltViewModel
class FaturaCartaoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    cartaoRepository: CartaoRepository,
    transacaoRepository: TransacaoRepository,
    parcelamentoRepository: ParcelamentoRepository,
    categoriaRepository: CategoriaRepository,
    contaRepository: ContaRepository,
    private val pagarFaturaCartaoUseCase: PagarFaturaCartaoUseCase
) : ViewModel() {

    private val cartaoId: Long = checkNotNull(savedStateHandle["cartaoId"])
    private val mesSelecionado = MutableStateFlow(mesAnoAtual())
    private val _eventos = MutableSharedFlow<FaturaCartaoEvento>()
    val eventos: SharedFlow<FaturaCartaoEvento> = _eventos.asSharedFlow()

    val uiState: StateFlow<FaturaCartaoUiState> = combine(
        cartaoRepository.getAll(),
        transacaoRepository.getAll(),
        parcelamentoRepository.getAll(),
        combine(
            categoriaRepository.getAll(),
            contaRepository.getAllAtivas(),
            contaRepository.getAll()
        ) { categorias, contasAtivas, contas -> Triple(categorias, contasAtivas, contas) },
        mesSelecionado
    ) { cartoes, transacoes, parcelamentos, categoriasContas, (mes, ano) ->
        val categorias = categoriasContas.first
        val contasAtivas = categoriasContas.second
        val contas = categoriasContas.third
        val cartao = cartoes.firstOrNull { it.id == cartaoId }
        if (cartao == null) {
            return@combine FaturaCartaoUiState(isLoading = false, mes = mes, ano = ano)
        }
        val categoriasMap = categorias.associate { it.id to it.nome }
        val contasMap = contas.associate { it.id to it.nome }
        val transacoesCartao = transacoes.filter {
            it.cartaoId == cartaoId &&
                it.tipo == "despesa" &&
                !RegrasFinanceiras.isPagamentoFatura(it)
        }
        val transacoesPorId = transacoesCartao.associateBy { it.id }
        val competenciaFatura = inicioMes(mes, ano)
        val totalPago = RegrasFinanceiras.totalPagoFatura(transacoes, cartaoId, competenciaFatura)
        val pagamentos = transacoes
            .filter { it.cartaoId == cartaoId }
            .filter { RegrasFinanceiras.pagamentoPertenceAFatura(it, competenciaFatura) }
            .map { it.toItemPagamento(contasMap) }

        val comprasAvulsas = transacoesCartao
            .filterNot { it.parcelado }
            .mapNotNull { transacao ->
                val vencimento = RegrasFinanceiras.dataFatura(transacao, cartao)
                if (!vencimento.estaNoMes(mes, ano)) return@mapNotNull null
                transacao.toItemFatura(
                    categoria = transacao.categoriaId?.let(categoriasMap::get) ?: "Sem categoria",
                    vencimento = vencimento,
                    pago = false
                )
            }
        val parcelas = parcelamentos
            .filter { it.cartaoId == cartaoId }
            .flatMap { parcelamento ->
                val transacao = transacoesPorId[parcelamento.transacaoPrincipalId]
                    ?: return@flatMap emptyList()
                parcelamento.itensDoMes(
                    transacao = transacao,
                    categoria = transacao.categoriaId?.let(categoriasMap::get) ?: "Sem categoria",
                    mes = mes,
                    ano = ano
                )
            }
        val itens = (comprasAvulsas + parcelas).sortedByDescending { it.dataMillis }
        val totalFatura = itens.sumOf { it.valor }
        val totalAberto = (totalFatura - totalPago).coerceAtLeast(0.0)
        val faturaPaga = totalFatura > 0.0 && totalAberto <= 0.009
        val itensExibidos = (if (faturaPaga) {
            itens.map { it.copy(pago = true, pagavel = false) }
        } else {
            itens
        }) + pagamentos
        FaturaCartaoUiState(
            isLoading = false,
            nomeCartao = cartao.nome,
            mes = mes,
            ano = ano,
            total = totalFatura,
            totalPago = totalPago,
            totalAberto = totalAberto,
            faturaPaga = faturaPaga,
            contasPagamento = contasAtivas.map { ContaPagamentoUi(it.id, it.nome) },
            itens = itensExibidos.sortedByDescending { it.dataMillis }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FaturaCartaoUiState()
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

    fun pagarFatura(formaPagamento: String, contaId: Long?, valorPagamento: Double) {
        viewModelScope.launch {
            val state = uiState.value
            if (!state.podePagar) return@launch
            val (inicio, fim) = intervaloMes(state.mes, state.ano)
            pagarFaturaCartaoUseCase(cartaoId, inicio, fim, formaPagamento, contaId, valorPagamento)
                .onSuccess { valor -> _eventos.emit(FaturaCartaoEvento.FaturaPaga(valor)) }
                .onFailure { erro ->
                    _eventos.emit(
                        FaturaCartaoEvento.Erro(erro.message ?: "Não foi possível pagar a fatura")
                    )
                }
        }
    }

    private fun Transacao.toItemFatura(
        categoria: String,
        vencimento: Long,
        pago: Boolean
    ) = ItemFaturaUi(
        chave = "compra-$id",
        descricao = observacao?.takeIf { it.isNotBlank() } ?: "Compra no cartao",
        categoria = categoria,
        data = formatarData(vencimento),
        dataMillis = vencimento,
        valor = valor,
        valorFormatado = "R$ %,.2f".format(valor),
        detalhe = "Compra a vista",
        pago = pago,
        pagavel = !pago
    )

    private fun Transacao.toItemPagamento(contas: Map<Long, String>) = ItemFaturaUi(
        chave = "pagamento-$id",
        descricao = RegrasFinanceiras.descricaoVisivel(this)?.takeIf { it.isNotBlank() }
            ?: "Pagamento de fatura",
        categoria = when (formaPagamento) {
            "conta" -> contaId?.let(contas::get) ?: "Conta"
            "dinheiro" -> "Dinheiro"
            else -> "Pagamento"
        },
        data = formatarData(dataCompetencia),
        dataMillis = dataCompetencia,
        valor = valor,
        valorFormatado = "R$ %,.2f".format(valor),
        detalhe = "Pagamento realizado",
        pago = true,
        pagavel = false,
        pagamento = true
    )

    private fun Parcelamento.itensDoMes(
        transacao: Transacao,
        categoria: String,
        mes: Int,
        ano: Int
    ): List<ItemFaturaUi> =
        (0 until totalParcelas).mapNotNull { indice ->
            val vencimento = dataParcela(indice)
            if (!vencimento.estaNoMes(mes, ano)) return@mapNotNull null
            ItemFaturaUi(
                chave = "parcela-$id-$indice",
                descricao = transacao.observacao?.takeIf { it.isNotBlank() } ?: "Compra parcelada",
                categoria = categoria,
                data = formatarData(vencimento),
                dataMillis = vencimento,
                valor = valorParcela,
                valorFormatado = "R$ %,.2f".format(valorParcela),
                detalhe = "Parcela ${indice + 1}/$totalParcelas",
                pago = indice < parcelasPagas,
                pagavel = indice == parcelasPagas
            )
        }

    private fun Parcelamento.dataParcela(indice: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dataPrimeiraParcela
            add(Calendar.MONTH, indice)
        }.timeInMillis

    private fun Long.estaNoMes(mes: Int, ano: Int): Boolean {
        val data = Calendar.getInstance().apply { timeInMillis = this@estaNoMes }
        return data.get(Calendar.MONTH) == mes && data.get(Calendar.YEAR) == ano
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

    private fun inicioMes(mes: Int, ano: Int): Long =
        Calendar.getInstance().apply {
            set(ano, mes, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

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
        return inicio to fim
    }
}
