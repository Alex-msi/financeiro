package com.example.financeiro.ui.relatorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Subcategoria
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import com.example.financeiro.domain.usecase.RestaurarBackupUseCase
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

data class CategoriaRelatorioUi(
    val nome: String,
    val valor: Double,
    val valorFormatado: String,
    val percentual: Float
)

data class EvolucaoMesUi(
    val label: String,
    val receitas: Float,
    val despesas: Float
)

data class LinhaExportacaoUi(
    val data: String,
    val tipo: String,
    val descricao: String,
    val categoria: String,
    val formaPagamento: String,
    val origem: String,
    val valor: Double
)

data class BackupDadosUi(
    val contas: List<Conta> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val subcategorias: List<Subcategoria> = emptyList(),
    val transacoes: List<Transacao> = emptyList(),
    val parcelamentos: List<Parcelamento> = emptyList()
)

data class RelatoriosUiState(
    val isLoading: Boolean = true,
    val mes: Int = Calendar.getInstance().get(Calendar.MONTH),
    val ano: Int = Calendar.getInstance().get(Calendar.YEAR),
    val receitas: Double = 0.0,
    val despesas: Double = 0.0,
    val saldo: Double = 0.0,
    val categorias: List<CategoriaRelatorioUi> = emptyList(),
    val evolucao: List<EvolucaoMesUi> = emptyList(),
    val linhasExportacao: List<LinhaExportacaoUi> = emptyList(),
    val backup: BackupDadosUi = BackupDadosUi()
) {
    val labelMes: String get() = "${nomeMes(mes)} $ano"
    val receitasFormatadas: String get() = formatarValor(receitas)
    val despesasFormatadas: String get() = formatarValor(despesas)
    val saldoFormatado: String get() = formatarValor(saldo)
    val semDespesas: Boolean get() = !isLoading && categorias.isEmpty()

    companion object {
        fun nomeMes(mes: Int): String = listOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )[mes.coerceIn(0, 11)]

        fun formatarValor(valor: Double): String = "R$ %,.2f".format(valor)
    }
}

@HiltViewModel
class RelatoriosViewModel @Inject constructor(
    transacaoRepository: TransacaoRepository,
    parcelamentoRepository: ParcelamentoRepository,
    categoriaRepository: CategoriaRepository,
    contaRepository: ContaRepository,
    cartaoRepository: CartaoRepository,
    private val restaurarBackupUseCase: RestaurarBackupUseCase
) : ViewModel() {

    private val mesSelecionado = MutableStateFlow(mesAnoAtual())
    private val _eventos = MutableSharedFlow<RelatoriosEvento>()
    val eventos: SharedFlow<RelatoriosEvento> = _eventos.asSharedFlow()
    private val categoriasESubcategorias = combine(
        categoriaRepository.getAll(),
        categoriaRepository.getAllSubcategorias()
    ) { categorias, subcategorias -> categorias to subcategorias }
    private val contasECartoes = combine(
        contaRepository.getAll(),
        cartaoRepository.getAll()
    ) { contas, cartoes -> contas to cartoes }

    val uiState: StateFlow<RelatoriosUiState> = combine(
        transacaoRepository.getAll(),
        parcelamentoRepository.getAll(),
        categoriasESubcategorias,
        contasECartoes,
        mesSelecionado
    ) { transacoes, parcelamentos, (categorias, subcategorias), contasCartoes, (mes, ano) ->
        val (contas, cartoes) = contasCartoes
        val nomesCategorias = categorias.associate { it.id to it.nome }
        val nomesSubcategorias = subcategorias.associate { it.id to it.nome }
        val nomesContas = contas.associate { it.id to it.nome }
        val nomesCartoes = cartoes.associate { it.id to it.nome }
        val movimentos = movimentosDoMes(
            transacoes,
            parcelamentos,
            mes,
            ano,
            nomesCategorias,
            nomesSubcategorias,
            nomesContas,
            nomesCartoes
        )
        val receitas = movimentos.filter { it.tipo == "receita" }.sumOf { it.valor }
        val despesasMes = movimentos.filter { it.tipo == "despesa" }
        val despesas = despesasMes.sumOf { it.valor }
        val categoriasUi = despesasMes
            .groupBy { it.categoriaId }
            .map { (categoriaId, itens) ->
                val valor = itens.sumOf { it.valor }
                CategoriaRelatorioUi(
                    nome = categoriaId?.let(nomesCategorias::get) ?: "Sem categoria",
                    valor = valor,
                    valorFormatado = RelatoriosUiState.formatarValor(valor),
                    percentual = if (despesas > 0) ((valor / despesas) * 100).toFloat() else 0f
                )
            }
            .sortedByDescending { it.valor }

        RelatoriosUiState(
            isLoading = false,
            mes = mes,
            ano = ano,
            receitas = receitas,
            despesas = despesas,
            saldo = receitas - despesas,
            categorias = categoriasUi,
            evolucao = evolucaoSeisMeses(transacoes, parcelamentos, mes, ano),
            linhasExportacao = movimentos.sortedBy { it.data }.map {
                LinhaExportacaoUi(
                    data = formatarData(it.data),
                    tipo = it.tipo,
                    descricao = it.descricao,
                    categoria = it.categoriaNome,
                    formaPagamento = it.formaPagamento,
                    origem = it.origem,
                    valor = it.valor
                )
            },
            backup = BackupDadosUi(
                contas = contas,
                cartoes = cartoes,
                categorias = categorias,
                subcategorias = subcategorias,
                transacoes = transacoes,
                parcelamentos = parcelamentos
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RelatoriosUiState()
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

    fun restaurarBackup(conteudo: String) {
        viewModelScope.launch {
            restaurarBackupUseCase(conteudo)
                .onSuccess { _eventos.emit(RelatoriosEvento.BackupRestaurado) }
                .onFailure { erro ->
                    _eventos.emit(
                        RelatoriosEvento.Erro(
                            erro.message ?: "Nao foi possivel restaurar o backup."
                        )
                    )
                }
        }
    }

    private fun movimentosDoMes(
        transacoes: List<Transacao>,
        parcelamentos: List<Parcelamento>,
        mes: Int,
        ano: Int,
        nomesCategorias: Map<Long, String> = emptyMap(),
        nomesSubcategorias: Map<Long, String> = emptyMap(),
        nomesContas: Map<Long, String> = emptyMap(),
        nomesCartoes: Map<Long, String> = emptyMap()
    ): List<MovimentoRelatorio> {
        val (inicio, fim) = intervaloMes(mes, ano)
        val transacoesPorId = transacoes.associateBy { it.id }
        val movimentosDiretos = transacoes
            .filter { it.dataCompetencia >= inicio && it.dataCompetencia < fim }
            .filterNot { it.tipo == "despesa" && it.parcelado }
            .filterNot(RegrasFinanceiras::isPagamentoFatura)
            .map {
                MovimentoRelatorio(
                    tipo = it.tipo,
                    valor = it.valor,
                    categoriaId = it.categoriaId,
                    data = it.dataCompetencia,
                descricao = RegrasFinanceiras.descricaoVisivel(it)?.takeIf(String::isNotBlank) ?: "Sem descrição",
                    categoriaNome = montarCategoria(it, nomesCategorias, nomesSubcategorias),
                    formaPagamento = it.formaPagamento,
                    origem = origemMovimento(it, nomesContas, nomesCartoes)
                )
            }
        val parcelasDoMes = parcelamentos.mapNotNull { parcelamento ->
            val transacao = transacoesPorId[parcelamento.transacaoPrincipalId] ?: return@mapNotNull null
            val indice = parcelamento.indiceNoMes(mes, ano)
            if (indice !in 0 until parcelamento.totalParcelas) {
                return@mapNotNull null
            }
            MovimentoRelatorio(
                tipo = "despesa",
                valor = parcelamento.valorParcela,
                categoriaId = transacao.categoriaId,
                data = parcelamento.dataParcela(indice),
                descricao = "${RegrasFinanceiras.descricaoVisivel(transacao) ?: "Compra parcelada"} (${indice + 1}/${parcelamento.totalParcelas})",
                categoriaNome = montarCategoria(transacao, nomesCategorias, nomesSubcategorias),
                formaPagamento = "cartao",
                origem = transacao.cartaoId?.let(nomesCartoes::get) ?: "Cartão"
            )
        }
        return movimentosDiretos + parcelasDoMes
    }

    private fun evolucaoSeisMeses(
        transacoes: List<Transacao>,
        parcelamentos: List<Parcelamento>,
        mes: Int,
        ano: Int
    ): List<EvolucaoMesUi> {
        val calendario = Calendar.getInstance().apply {
            set(ano, mes, 1)
            add(Calendar.MONTH, -5)
        }
        return List(6) {
            val mesItem = calendario.get(Calendar.MONTH)
            val anoItem = calendario.get(Calendar.YEAR)
            val movimentos = movimentosDoMes(transacoes, parcelamentos, mesItem, anoItem)
            val item = EvolucaoMesUi(
                label = RelatoriosUiState.nomeMes(mesItem).take(3),
                receitas = movimentos.filter { movimento -> movimento.tipo == "receita" }
                    .sumOf { movimento -> movimento.valor }.toFloat(),
                despesas = movimentos.filter { movimento -> movimento.tipo == "despesa" }
                    .sumOf { movimento -> movimento.valor }.toFloat()
            )
            calendario.add(Calendar.MONTH, 1)
            item
        }
    }

    private fun Parcelamento.indiceNoMes(mes: Int, ano: Int): Int {
        val primeira = Calendar.getInstance().apply { timeInMillis = dataPrimeiraParcela }
        return (ano - primeira.get(Calendar.YEAR)) * 12 + mes - primeira.get(Calendar.MONTH)
    }

    private fun Parcelamento.dataParcela(indice: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dataPrimeiraParcela
            add(Calendar.MONTH, indice)
        }.timeInMillis

    private fun montarCategoria(
        transacao: Transacao,
        categorias: Map<Long, String>,
        subcategorias: Map<Long, String>
    ): String {
        val categoria = transacao.categoriaId?.let(categorias::get) ?: "Sem categoria"
        val subcategoria = transacao.subcategoriaId?.let(subcategorias::get)
        return if (subcategoria == null) categoria else "$categoria / $subcategoria"
    }

    private fun origemMovimento(
        transacao: Transacao,
        contas: Map<Long, String>,
        cartoes: Map<Long, String>
    ): String =
        when (transacao.formaPagamento) {
            "conta" -> transacao.contaId?.let(contas::get) ?: "Conta"
            "cartao" -> transacao.cartaoId?.let(cartoes::get) ?: "Cartão"
            "dinheiro" -> "Dinheiro"
            else -> transacao.formaPagamento
        }

    private fun formatarData(valor: Long): String {
        val data = Calendar.getInstance().apply { timeInMillis = valor }
        return "%02d/%02d/%04d".format(
            data.get(Calendar.DAY_OF_MONTH),
            data.get(Calendar.MONTH) + 1,
            data.get(Calendar.YEAR)
        )
    }

    private fun intervaloMes(mes: Int, ano: Int): Pair<Long, Long> {
        val inicio = Calendar.getInstance().apply {
            set(ano, mes, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val fim = Calendar.getInstance().apply {
            set(ano, mes, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }.timeInMillis
        return inicio to fim
    }

    private data class MovimentoRelatorio(
        val tipo: String,
        val valor: Double,
        val categoriaId: Long?,
        val data: Long = 0L,
        val descricao: String = "",
        val categoriaNome: String = "Sem categoria",
        val formaPagamento: String = "",
        val origem: String = ""
    )

    private fun mesAnoAtual(): Pair<Int, Int> {
        val agora = Calendar.getInstance()
        return agora.get(Calendar.MONTH) to agora.get(Calendar.YEAR)
    }
}

sealed class RelatoriosEvento {
    data object BackupRestaurado : RelatoriosEvento()
    data class Erro(val mensagem: String) : RelatoriosEvento()
}
