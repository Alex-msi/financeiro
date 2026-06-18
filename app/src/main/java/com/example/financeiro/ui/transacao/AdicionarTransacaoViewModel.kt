package com.example.financeiro.ui.transacao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.ContaRepository
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

    // Seleções
    val categoriaSelecionadaId: Long? = null,
    val categoriaSelecionadaNome: String = "",
    val contaSelecionadaId: Long? = null,
    val contaSelecionadaNome: String = "",
    val cartaoSelecionadoId: Long? = null,
    val cartaoSelecionadoNome: String = "",

    // Listas para os dropdowns
    val categorias: List<Categoria> = emptyList(),
    val contas: List<Conta> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),

    // Feedback
    val erro: String? = null,
    val salvoCom: Boolean = false
) {
    val mostrarConta: Boolean get() = formaPagamento == "conta"
    val mostrarCartao: Boolean get() = formaPagamento == "cartao"
    val titulo: String get() = if (isEdicao) "Editar Transação" else "Nova Transação"
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class AdicionarTransacaoViewModel @Inject constructor(
    private val salvarTransacaoUseCase: SalvarTransacaoUseCase,
    private val transacaoRepository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdicionarTransacaoUiState(
        dataFormatada = dataParaString(System.currentTimeMillis())
    ))
    val uiState: StateFlow<AdicionarTransacaoUiState> = _uiState.asStateFlow()

    // ID da transação sendo editada (null = nova)
    private var transacaoEditandoId: Long? = null

    init {
        carregarListas()
    }

    // ─── Inicialização ────────────────────────────────────────────────────────

    private fun carregarListas() {
        viewModelScope.launch {
            launch {
                categoriaRepository.getAll().collect { lista ->
                    _uiState.update { it.copy(categorias = lista) }
                }
            }
            launch {
                contaRepository.getAllAtivas().collect { lista ->
                    _uiState.update { state ->
                        val contaId = state.contaSelecionadaId ?: lista.firstOrNull()?.id
                        val contaNome = lista.firstOrNull { it.id == contaId }?.nome ?: ""
                        state.copy(contas = lista, contaSelecionadaId = contaId, contaSelecionadaNome = contaNome)
                    }
                }
            }
            launch {
                cartaoRepository.getAllAtivos().collect { lista ->
                    _uiState.update { state ->
                        val cartaoId = state.cartaoSelecionadoId ?: lista.firstOrNull()?.id
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
                _uiState.update { state ->
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
                        contaSelecionadaId = transacao.contaId,
                        cartaoSelecionadoId = transacao.cartaoId
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
            contaSelecionadaId = if (forma == "conta") contaPadrao else it.contaSelecionadaId,
            contaSelecionadaNome = if (forma == "conta") contaNomePadrao else it.contaSelecionadaNome,
            cartaoSelecionadoId = if (tipo == "receita") null else it.cartaoSelecionadoId,
            cartaoSelecionadoNome = if (tipo == "receita") "" else it.cartaoSelecionadoNome,
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
                erro = null
            )
        }

    fun onDataChanged(epochMillis: Long) =
        _uiState.update { it.copy(dataCompetencia = epochMillis, dataFormatada = dataParaString(epochMillis)) }

    fun onObservacaoChanged(obs: String) = _uiState.update { it.copy(observacao = obs) }

    fun onCategoriaSelected(categoria: Categoria) =
        _uiState.update { it.copy(categoriaSelecionadaId = categoria.id, categoriaSelecionadaNome = categoria.nome) }

    fun onContaSelected(conta: Conta) =
        _uiState.update { it.copy(contaSelecionadaId = conta.id, contaSelecionadaNome = conta.nome) }

    fun onCartaoSelected(cartao: Cartao) =
        _uiState.update { it.copy(cartaoSelecionadoId = cartao.id, cartaoSelecionadoNome = cartao.nome) }

    // ─── Salvar ───────────────────────────────────────────────────────────────

    fun salvar() {
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
                subcategoriaId = null,
                formaPagamento = state.formaPagamento,
                cartaoId = cartaoId,
                contaId = contaId,
                parcelado = false,
                numeroParcelas = 1,
                parcelaAtual = 1,
                observacao = state.observacao.ifBlank { null },
                criadoEm = System.currentTimeMillis()
            )

            val resultado = salvarTransacaoUseCase(transacao)
            resultado.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, salvoCom = true) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, erro = e.message ?: "Erro ao salvar.") } }
            )
        }
    }

    fun limparErro() = _uiState.update { it.copy(erro = null) }

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
    }
}
