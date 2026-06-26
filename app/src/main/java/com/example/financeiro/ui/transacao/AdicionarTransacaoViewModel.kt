package com.example.financeiro.ui.transacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.model.Subcategoria
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import com.example.financeiro.domain.usecase.SalvarTransacaoUseCase
import com.example.financeiro.domain.model.Transacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

// ─── UiState ─────────────────────────────────────────────────────────────────

data class AdicionarTransacaoUiState(
    val isLoading: Boolean = false,
    val isEdicao: Boolean = false,

    // Campos do formulário
    val valor: String = "",
    val tipo: String = "despesa",               // "receita" ou "despesa"
    val dataCompetencia: Long = System.currentTimeMillis(),
    val dataFormatada: String = "",
    val formaPagamento: String = "conta",        // "conta", "cartao", "dinheiro"
    val observacao: String = "",
    val parcelado: Boolean = false,
    val numeroParcelas: String = "",
    val parcelasPagas: String = "",
    val recorrente: Boolean = false,
    val quantidadeRecorrencias: String = "",
    val recorrenciaId: String? = null,
    val recorrenciaIndice: Int? = null,

    // Seleções
    val categoriaSelecionadaId: Long? = null,
    val categoriaSelecionadaNome: String = "",
    val subcategoriaSelecionadaId: Long? = null,
    val subcategoriaSelecionadaNome: String = "",
    val contaSelecionadaId: Long? = null,
    val contaSelecionadaNome: String = "",
    val cartaoSelecionadoId: Long? = null,
    val cartaoSelecionadoNome: String = "",

    // Listas para os dropdowns
    val categorias: List<Categoria> = emptyList(),
    val subcategorias: List<Subcategoria> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),

    // Feedback
    val erro: String? = null,
    val salvoCom: Boolean = false
) {
    val mostrarConta: Boolean get() = formaPagamento == "conta"
    val mostrarCartao: Boolean get() = formaPagamento == "cartao"
    val mostrarParcelamento: Boolean get() = tipo == "despesa" && formaPagamento == "cartao"
    val mostrarNumeroParcelas: Boolean get() = mostrarParcelamento && parcelado
    val mostrarParcelasPagas: Boolean get() = isEdicao && mostrarNumeroParcelas
    val mostrarRecorrencia: Boolean get() = !isEdicao && !parcelado
    val mostrarQuantidadeRecorrencias: Boolean get() = mostrarRecorrencia && recorrente
    val podeEscolherEscopoRecorrencia: Boolean get() = isEdicao && recorrenciaId != null
    val titulo: String get() = if (isEdicao) "Editar Transação" else "Nova Transação"
}

enum class EscopoEdicaoRecorrencia {
    SOMENTE_ESTA,
    ESTA_E_PROXIMAS
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class AdicionarTransacaoViewModel @Inject constructor(
    private val salvarTransacaoUseCase: SalvarTransacaoUseCase,
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val parcelamentoRepository: ParcelamentoRepository,
    private val cartaoRepository: CartaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdicionarTransacaoUiState(
        dataFormatada = dataParaString(System.currentTimeMillis())
    ))
    val uiState: StateFlow<AdicionarTransacaoUiState> = _uiState.asStateFlow()

    // ID da transação sendo editada (null = nova)
    private var transacaoEditandoId: Long? = null
    private var transacaoCriadoEm: Long? = null
    private var dataOriginalEdicao: Long? = null

    init {
        carregarListas()
    }

    // ─── Inicialização ────────────────────────────────────────────────────────

    private fun carregarListas() {
        viewModelScope.launch {
            launch {
                categoriaRepository.getAll().collect { lista ->
                    _uiState.update { state ->
                        val categoriaNome = lista.firstOrNull { it.id == state.categoriaSelecionadaId }?.nome
                            ?: state.categoriaSelecionadaNome
                        state.copy(categorias = lista, categoriaSelecionadaNome = categoriaNome)
                    }
                }
            }
            launch {
                categoriaRepository.getAllSubcategorias().collect { lista ->
                    _uiState.update { state ->
                        val nome = lista.firstOrNull { it.id == state.subcategoriaSelecionadaId }?.nome
                            ?: state.subcategoriaSelecionadaNome
                        state.copy(subcategorias = lista, subcategoriaSelecionadaNome = nome)
                    }
                }
            }
            launch {
                contaRepository.getAllAtivas().collect { lista ->
                    _uiState.update { state ->
                        val contaId = state.contaSelecionadaId
                            ?.takeIf { id -> lista.any { it.id == id } }
                            ?: lista.firstOrNull()?.id
                        val contaNome = lista.firstOrNull { it.id == contaId }?.nome ?: ""
                        state.copy(contas = lista, contaSelecionadaId = contaId, contaSelecionadaNome = contaNome)
                    }
                }
            }
            launch {
                cartaoRepository.getAll().collect { lista ->
                    _uiState.update { state ->
                        val cartaoId = state.cartaoSelecionadoId
                            ?.takeIf { id -> lista.any { it.id == id } }
                            ?: lista.firstOrNull()?.id
                        val cartaoNome = lista.firstOrNull { it.id == cartaoId }?.nome ?: ""
                        state.copy(cartoes = lista, cartaoSelecionadoId = cartaoId, cartaoSelecionadoNome = cartaoNome)
                    }
                }
            }
        }
    }

    /**
     * Chamado pelo Fragment quando recebe um transacaoId via args.
     * Carrega os dados da transação existente para preencher o formulário.
     */
    fun carregarTransacao(id: Long) {
        if (transacaoEditandoId == id) return
        transacaoEditandoId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val transacao = transacaoRepository.getById(id) ?: return@launch
                val parcelamento = parcelamentoRepository.getByTransacao(id).first().firstOrNull()
                _uiState.update { state ->
                    val categoriaNome = state.categorias.firstOrNull { it.id == transacao.categoriaId }?.nome ?: ""
                    val subcategoriaNome =
                        state.subcategorias.firstOrNull { it.id == transacao.subcategoriaId }?.nome ?: ""
                    val contaNome = state.contas.firstOrNull { it.id == transacao.contaId }?.nome ?: ""
                    val cartaoNome = state.cartoes.firstOrNull { it.id == transacao.cartaoId }?.nome ?: ""
                    transacaoCriadoEm = transacao.criadoEm
                    dataOriginalEdicao = transacao.dataCompetencia
                    state.copy(
                        isLoading = false,
                        isEdicao = true,
                        valor = transacao.valor.toString(),
                        tipo = transacao.tipo,
                        dataCompetencia = transacao.dataCompetencia,
                        dataFormatada = dataParaString(transacao.dataCompetencia),
                        formaPagamento = transacao.formaPagamento,
                        observacao = transacao.observacao ?: "",
                        categoriaSelecionadaId = transacao.categoriaId,
                        categoriaSelecionadaNome = categoriaNome,
                        subcategoriaSelecionadaId = transacao.subcategoriaId,
                        subcategoriaSelecionadaNome = subcategoriaNome,
                        contaSelecionadaId = transacao.contaId,
                        contaSelecionadaNome = contaNome,
                        cartaoSelecionadoId = transacao.cartaoId,
                        cartaoSelecionadoNome = cartaoNome,
                        parcelado = transacao.parcelado,
                        numeroParcelas = if (transacao.parcelado) transacao.numeroParcelas.toString() else "",
                        parcelasPagas = parcelamento?.parcelasPagas?.toString() ?: "",
                        recorrente = false,
                        quantidadeRecorrencias = "",
                        recorrenciaId = transacao.recorrenciaId,
                        recorrenciaIndice = transacao.recorrenciaIndice
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, erro = "Erro ao carregar transação.") }
            }
        }
    }

    // ─── Atualização dos campos ───────────────────────────────────────────────

    fun onValorChanged(valor: String) = _uiState.update { it.copy(valor = valor, erro = null) }

    fun onTipoChanged(tipo: String) = _uiState.update { it ->
        // Ao mudar para receita, força forma de pagamento para "conta"
        val forma = if (tipo == "receita" && it.formaPagamento == "cartao") "conta" else it.formaPagamento
        val contaPadrao = it.contaSelecionadaId ?: it.contas.firstOrNull()?.id
        val contaNomePadrao = it.contas.firstOrNull { conta -> conta.id == contaPadrao }?.nome ?: ""
        it.copy(
            tipo = tipo,
            formaPagamento = forma,
            categoriaSelecionadaId = null,
            categoriaSelecionadaNome = "",
            subcategoriaSelecionadaId = null,
            subcategoriaSelecionadaNome = "",
            contaSelecionadaId = if (forma == "conta") contaPadrao else it.contaSelecionadaId,
            contaSelecionadaNome = if (forma == "conta") contaNomePadrao else it.contaSelecionadaNome,
            cartaoSelecionadoId = if (tipo == "receita") null else it.cartaoSelecionadoId,
            cartaoSelecionadoNome = if (tipo == "receita") "" else it.cartaoSelecionadoNome,
            parcelado = if (tipo == "receita") false else it.parcelado,
            numeroParcelas = if (tipo == "receita") "" else it.numeroParcelas,
            recorrente = if (tipo == "receita") it.recorrente else it.recorrente,
            erro = null
        )
    }

    fun onFormaPagamentoChanged(forma: String) =
        _uiState.update {
            val contaPadrao = it.contaSelecionadaId ?: it.contas.firstOrNull()?.id
            val contaNomePadrao = it.contas.firstOrNull { conta -> conta.id == contaPadrao }?.nome ?: ""
            val cartaoPadrao = it.cartaoSelecionadoId ?: it.cartoes.firstOrNull()?.id
            val cartaoNomePadrao = it.cartoes.firstOrNull { cartao -> cartao.id == cartaoPadrao }?.nome ?: ""

            it.copy(
                formaPagamento = forma,
                contaSelecionadaId = if (forma == "conta") contaPadrao else null,
                contaSelecionadaNome = if (forma == "conta") contaNomePadrao else "",
                cartaoSelecionadoId = if (forma == "cartao") cartaoPadrao else null,
                cartaoSelecionadoNome = if (forma == "cartao") cartaoNomePadrao else "",
                parcelado = if (forma == "cartao" && it.tipo == "despesa") it.parcelado else false,
                numeroParcelas = if (forma == "cartao" && it.tipo == "despesa") it.numeroParcelas else "",
                recorrente = if (forma == "cartao" && it.parcelado) false else it.recorrente,
                erro = null
            )
        }

    fun onDataChanged(epochMillis: Long) =
        _uiState.update { it.copy(dataCompetencia = epochMillis, dataFormatada = dataParaString(epochMillis)) }

    fun onObservacaoChanged(obs: String) = _uiState.update { it.copy(observacao = obs) }

    fun onParceladoChanged(parcelado: Boolean) =
        _uiState.update {
            it.copy(
                parcelado = parcelado,
                numeroParcelas = if (parcelado) it.numeroParcelas else "",
                parcelasPagas = if (parcelado) it.parcelasPagas else "",
                recorrente = if (parcelado) false else it.recorrente,
                quantidadeRecorrencias = if (parcelado) "" else it.quantidadeRecorrencias,
                erro = null
            )
        }

    fun onRecorrenteChanged(recorrente: Boolean) =
        _uiState.update {
            it.copy(
                recorrente = recorrente,
                quantidadeRecorrencias = if (recorrente) it.quantidadeRecorrencias else "",
                erro = null
            )
        }

    fun onNumeroParcelasChanged(numeroParcelas: String) =
        _uiState.update {
            it.copy(numeroParcelas = numeroParcelas.filter { char -> char.isDigit() }, erro = null)
        }

    fun onParcelasPagasChanged(parcelasPagas: String) =
        _uiState.update {
            it.copy(parcelasPagas = parcelasPagas.filter { char -> char.isDigit() }, erro = null)
        }

    fun onQuantidadeRecorrenciasChanged(quantidade: String) =
        _uiState.update {
            it.copy(quantidadeRecorrencias = quantidade.filter { char -> char.isDigit() }, erro = null)
        }

    fun onCategoriaSelected(categoria: Categoria) =
        _uiState.update {
            it.copy(
                categoriaSelecionadaId = categoria.id,
                categoriaSelecionadaNome = categoria.nome,
                subcategoriaSelecionadaId = null,
                subcategoriaSelecionadaNome = ""
            )
        }

    fun onSubcategoriaSelected(subcategoria: Subcategoria) =
        _uiState.update {
            it.copy(
                subcategoriaSelecionadaId = subcategoria.id,
                subcategoriaSelecionadaNome = subcategoria.nome
            )
        }

    fun onContaSelected(conta: Conta) =
        _uiState.update { it.copy(contaSelecionadaId = conta.id, contaSelecionadaNome = conta.nome) }

    fun onCartaoSelected(cartao: Cartao) =
        _uiState.update { it.copy(cartaoSelecionadoId = cartao.id, cartaoSelecionadoNome = cartao.nome) }

    // ─── Salvar ───────────────────────────────────────────────────────────────

    fun salvar(escopoRecorrencia: EscopoEdicaoRecorrencia = EscopoEdicaoRecorrencia.SOMENTE_ESTA) {
        val state = _uiState.value
        val valorDouble = state.valor.replace(",", ".").toDoubleOrNull()

        if (valorDouble == null || valorDouble <= 0.0) {
            _uiState.update { it.copy(erro = "Informe um valor válido.") }
            return
        }
        if (state.formaPagamento == "conta" && state.contaSelecionadaId == null) {
            _uiState.update { it.copy(erro = "Selecione uma conta.") }
            return
        }
        if (state.formaPagamento == "cartao" && state.cartaoSelecionadoId == null) {
            _uiState.update { it.copy(erro = "Selecione um cartão.") }
            return
        }

        val numeroParcelasInt = if (state.parcelado) state.numeroParcelas.toIntOrNull() else 1
        if (state.parcelado && (numeroParcelasInt == null || numeroParcelasInt <= 1)) {
            _uiState.update { it.copy(erro = "Informe mais de 1 parcela.") }
            return
        }
        val parcelasPagasInt = if (state.mostrarParcelasPagas) {
            state.parcelasPagas.toIntOrNull() ?: 0
        } else {
            0
        }
        if (state.mostrarParcelasPagas && numeroParcelasInt != null && parcelasPagasInt > numeroParcelasInt) {
            _uiState.update { it.copy(erro = "Parcelas pagas não pode ser maior que o total.") }
            return
        }
        val quantidadeRecorrenciasInt = if (state.recorrente) {
            state.quantidadeRecorrencias.toIntOrNull()
        } else {
            1
        }
        if (state.recorrente && (quantidadeRecorrenciasInt == null || quantidadeRecorrenciasInt <= 1)) {
            _uiState.update { it.copy(erro = "Informe pelo menos 2 meses para repetir.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, erro = null) }

        viewModelScope.launch {
            val contaId = if (state.formaPagamento == "conta") state.contaSelecionadaId else null
            val cartaoId = if (state.formaPagamento == "cartao") state.cartaoSelecionadoId else null

            val transacao = Transacao(
                id = transacaoEditandoId ?: 0L,
                valor = valorDouble,
                dataCompetencia = state.dataCompetencia,
                tipo = state.tipo,
                categoriaId = state.categoriaSelecionadaId,
                subcategoriaId = state.subcategoriaSelecionadaId,
                formaPagamento = state.formaPagamento,
                cartaoId = cartaoId,
                contaId = contaId,
                parcelado = state.parcelado,
                numeroParcelas = numeroParcelasInt ?: 1,
                parcelaAtual = 1,
                observacao = state.observacao.ifBlank { null },
                recorrenciaId = state.recorrenciaId,
                recorrenciaIndice = state.recorrenciaIndice,
                criadoEm = transacaoCriadoEm ?: System.currentTimeMillis()
            )

            val resultado = if (state.recorrente && transacaoEditandoId == null) {
                salvarRecorrencias(transacao, quantidadeRecorrenciasInt ?: 1)
            } else if (
                escopoRecorrencia == EscopoEdicaoRecorrencia.ESTA_E_PROXIMAS &&
                transacao.recorrenciaId != null &&
                transacao.recorrenciaIndice != null
            ) {
                salvarEstaEProximas(transacao)
            } else {
                salvarTransacaoUseCase(transacao)
            }
            resultado.fold(
                onSuccess = {
                    atualizarParcelasPagasSeNecessario(transacao.id, parcelasPagasInt)
                    _uiState.update { it.copy(isLoading = false, salvoCom = true) }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, erro = e.message ?: "Erro ao salvar.") } }
            )
        }
    }

    fun limparErro() = _uiState.update { it.copy(erro = null) }

    private suspend fun salvarRecorrencias(transacao: Transacao, quantidade: Int): Result<Unit> = runCatching {
        val recorrenciaId = UUID.randomUUID().toString()
        for (indice in 0 until quantidade) {
            salvarTransacaoUseCase(
                transacao.copy(
                    id = 0L,
                    dataCompetencia = adicionarMeses(transacao.dataCompetencia, indice),
                    recorrenciaId = recorrenciaId,
                    recorrenciaIndice = indice,
                    criadoEm = System.currentTimeMillis() + indice
                )
            ).getOrThrow()
        }
    }

    private suspend fun salvarEstaEProximas(transacao: Transacao): Result<Unit> = runCatching {
        val recorrenciaId = transacao.recorrenciaId ?: return@runCatching
        val indiceBase = transacao.recorrenciaIndice ?: return@runCatching
        val transacoesDaSerie = transacaoRepository.getAll().first()
            .filter { it.recorrenciaId == recorrenciaId }
            .filter { (it.recorrenciaIndice ?: -1) >= indiceBase }

        transacoesDaSerie.forEach { ocorrencia ->
            val indiceOcorrencia = ocorrencia.recorrenciaIndice ?: indiceBase
            val mesesDepois = indiceOcorrencia - indiceBase
            salvarTransacaoUseCase(
                ocorrencia.copy(
                    valor = transacao.valor,
                    dataCompetencia = adicionarMeses(transacao.dataCompetencia, mesesDepois),
                    tipo = transacao.tipo,
                    categoriaId = transacao.categoriaId,
                    subcategoriaId = transacao.subcategoriaId,
                    formaPagamento = transacao.formaPagamento,
                    cartaoId = transacao.cartaoId,
                    contaId = transacao.contaId,
                    parcelado = transacao.parcelado,
                    numeroParcelas = transacao.numeroParcelas,
                    parcelaAtual = transacao.parcelaAtual,
                    observacao = transacao.observacao
                )
            ).getOrThrow()
        }
    }

    private suspend fun atualizarParcelasPagasSeNecessario(transacaoId: Long, parcelasPagas: Int) {
        if (transacaoId <= 0) return
        val parcelamento = parcelamentoRepository.getByTransacao(transacaoId).first().firstOrNull() ?: return
        parcelamentoRepository.update(
            parcelamento.copy(parcelasPagas = parcelasPagas.coerceIn(0, parcelamento.totalParcelas))
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    companion object {
        fun dataParaString(epochMillis: Long): String {
            val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
            return "%02d/%02d/%04d".format(
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR)
            )
        }

        fun adicionarMeses(epochMillis: Long, meses: Int): Long =
            Calendar.getInstance().apply {
                timeInMillis = epochMillis
                add(Calendar.MONTH, meses)
            }.timeInMillis
    }
}
