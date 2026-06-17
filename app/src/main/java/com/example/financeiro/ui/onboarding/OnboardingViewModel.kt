package com.example.financeiro.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── State classes (abaixo do ViewModel por convenção de leitura) ─────────────
// O arquivo se chama OnboardingViewModel.kt — classe principal é OnboardingViewModel.
// As data classes de estado ficam no mesmo arquivo por coesão.

data class OnboardingUiState(
    val currentStep: Int = 0,               // 0=boas-vindas, 1=contas, 2=cartões, 3=parcelamentos, 4=conclusão
    val contas: List<Conta> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),
    val parcelamentosPendentes: List<ParcelamentoRascunho> = emptyList(),
    val isLoading: Boolean = false,
    val erro: String? = null,
    val onboardingConcluido: Boolean = false
)

/** Rascunho de parcelamento criado durante o onboarding antes de persistir */
data class ParcelamentoRascunho(
    val descricao: String,
    val valorParcela: Double,
    val parcelasRestantes: Int,
    val cartaoId: Long,
    val cartaoNome: String
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoRepository,
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val totalSteps = 5  // 0..4

    init {
        carregarDadosExistentes()
    }

    private fun carregarDadosExistentes() {
        viewModelScope.launch {
            try {
                val contas = contaRepository.getAllAtivas().first()
                val cartoes = cartaoRepository.getAllAtivos().first()
                _uiState.update {
                    it.copy(contas = contas, cartoes = cartoes, erro = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(erro = "Erro ao carregar dados iniciais: ${e.message}") }
            }
        }
    }

    // ─── Navegação entre passos ────────────────────────────────────────────────

    fun irParaProximoPasso() {
        val atual = _uiState.value.currentStep
        if (atual < totalSteps - 1) {
            _uiState.update { it.copy(currentStep = atual + 1, erro = null) }
        }
    }

    fun irParaPassoAnterior() {
        val atual = _uiState.value.currentStep
        if (atual > 0) {
            _uiState.update { it.copy(currentStep = atual - 1, erro = null) }
        }
    }

    fun pularPasso() {
        irParaProximoPasso()
    }

    // ─── Contas ────────────────────────────────────────────────────────────────

    fun adicionarConta(nome: String, tipo: String, saldoAtual: Double) {
        if (nome.isBlank()) {
            _uiState.update { it.copy(erro = "Informe o nome da conta.") }
            return
        }
        val agora = System.currentTimeMillis()
        val conta = Conta(
            id = 0L,
            nome = nome.trim(),
            tipo = tipo,
            saldoInicial = saldoAtual,
            saldoAtual = saldoAtual,
            ativa = true,
            criadoEm = agora
        )
        viewModelScope.launch {
            try {
                val contaId = contaRepository.insert(conta)
                val novaLista = _uiState.value.contas + conta.copy(id = contaId)
                _uiState.update { it.copy(contas = novaLista, erro = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(erro = "Erro ao salvar conta: ${e.message}") }
            }
        }
    }

    fun removerConta(index: Int) {
        val lista = _uiState.value.contas.toMutableList()
        if (index in lista.indices) {
            lista.removeAt(index)
            _uiState.update { it.copy(contas = lista) }
        }
    }

    fun podeContinuarContas(): Boolean = _uiState.value.contas.isNotEmpty()

    // ─── Cartões ───────────────────────────────────────────────────────────────

    fun adicionarCartao(
        nome: String,
        limiteTotal: Double,
        diaFechamento: Int,
        diaVencimento: Int
    ) {
        if (nome.isBlank()) {
            _uiState.update { it.copy(erro = "Informe o nome do cartão.") }
            return
        }
        if (diaFechamento !in 1..31 || diaVencimento !in 1..31) {
            _uiState.update { it.copy(erro = "Dias de fechamento/vencimento devem ser entre 1 e 31.") }
            return
        }
        val agora = System.currentTimeMillis()
        val cartao = Cartao(
            id = 0L,
            nome = nome.trim(),
            limiteTotal = limiteTotal,
            diaFechamento = diaFechamento,
            diaVencimento = diaVencimento,
            ativo = true,
            criadoEm = agora
        )
        viewModelScope.launch {
            try {
                val cartaoId = cartaoRepository.insert(cartao)
                val novaLista = _uiState.value.cartoes + cartao.copy(id = cartaoId)
                _uiState.update { it.copy(cartoes = novaLista, erro = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(erro = "Erro ao salvar cartão: ${e.message}") }
            }
        }
    }

    fun removerCartao(index: Int) {
        val lista = _uiState.value.cartoes.toMutableList()
        if (index in lista.indices) {
            lista.removeAt(index)
            _uiState.update { it.copy(cartoes = lista) }
        }
    }

    // ─── Parcelamentos ─────────────────────────────────────────────────────────

    fun adicionarParcelamentoRascunho(
        descricao: String,
        valorParcela: Double,
        parcelasRestantes: Int,
        cartaoId: Long,
        cartaoNome: String
    ) {
        if (descricao.isBlank()) {
            _uiState.update { it.copy(erro = "Informe a descrição do parcelamento.") }
            return
        }
        if (valorParcela <= 0.0) {
            _uiState.update { it.copy(erro = "O valor da parcela deve ser maior que zero.") }
            return
        }
        if (parcelasRestantes <= 0) {
            _uiState.update { it.copy(erro = "Informe ao menos 1 parcela restante.") }
            return
        }
        val rascunho = ParcelamentoRascunho(
            descricao = descricao.trim(),
            valorParcela = valorParcela,
            parcelasRestantes = parcelasRestantes,
            cartaoId = cartaoId,
            cartaoNome = cartaoNome
        )
        _uiState.update {
            it.copy(parcelamentosPendentes = it.parcelamentosPendentes + rascunho, erro = null)
        }
    }

    fun removerParcelamentoRascunho(index: Int) {
        val lista = _uiState.value.parcelamentosPendentes.toMutableList()
        if (index in lista.indices) {
            lista.removeAt(index)
            _uiState.update { it.copy(parcelamentosPendentes = lista) }
        }
    }

    // ─── Conclusão ─────────────────────────────────────────────────────────────

    /**
     * Persiste os parcelamentos em rascunho e marca o onboarding como concluído.
     * Cada rascunho gera:
     *  - 1 TransacaoEntity (tipo="despesa", forma_pagamento="cartao", parcelado=true)
     *  - 1 ParcelamentoEntity com total_parcelas = parcelasRestantes e parcelas_pagas = 0
     */
    fun concluirOnboarding(salvarPrefs: () -> Unit) {
        _uiState.update { it.copy(isLoading = true, erro = null) }
        viewModelScope.launch {
            try {
                val agora = System.currentTimeMillis()
                _uiState.value.parcelamentosPendentes.forEach { rascunho ->
                    val transacao = Transacao(
                        id = 0L,
                        valor = rascunho.valorParcela * rascunho.parcelasRestantes,
                        dataCompetencia = agora,
                        tipo = "despesa",
                        categoriaId = null,
                        subcategoriaId = null,
                        formaPagamento = "cartao",
                        cartaoId = rascunho.cartaoId,
                        contaId = null,
                        parcelado = true,
                        numeroParcelas = rascunho.parcelasRestantes,
                        parcelaAtual = 1,
                        observacao = rascunho.descricao,
                        criadoEm = agora
                    )
                    val transacaoId = transacaoRepository.insert(transacao)

                    val parcelamento = Parcelamento(
                        id = 0L,
                        transacaoPrincipalId = transacaoId,
                        valorParcela = rascunho.valorParcela,
                        totalParcelas = rascunho.parcelasRestantes,
                        parcelasPagas = 0,
                        dataPrimeiraParcela = agora,
                        cartaoId = rascunho.cartaoId,
                        criadoEm = agora
                    )
                    parcelamentoRepository.insert(parcelamento)
                }

                salvarPrefs()
                _uiState.update { it.copy(isLoading = false, onboardingConcluido = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, erro = "Erro ao concluir onboarding: ${e.message}")
                }
            }
        }
    }

    fun limparErro() {
        _uiState.update { it.copy(erro = null) }
    }
}
