package com.example.financeiro.ui.carteira

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.finance.RegrasFinanceiras
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Conta
import com.example.financeiro.domain.model.Parcelamento
import com.example.financeiro.domain.model.Transacao
import com.example.financeiro.domain.repository.CartaoRepository
import com.example.financeiro.domain.repository.ContaRepository
import com.example.financeiro.domain.repository.ParcelamentoRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CarteiraUiState(
    val isLoading: Boolean = true,
    val contas: List<ContaUi> = emptyList(),
    val cartoes: List<CartaoUi> = emptyList(),
    val parcelamentos: List<ParcelamentoUi> = emptyList()
) {
    val isEmpty: Boolean get() =
        !isLoading && contas.isEmpty() && cartoes.isEmpty() && parcelamentos.isEmpty()
}

data class ContaUi(
    val conta: Conta,
    val id: Long,
    val nome: String,
    val tipo: String,
    val saldo: Double,
    val saldoFormatado: String,
    val detalhe: String
)

data class CartaoUi(
    val cartao: Cartao,
    val id: Long,
    val nome: String,
    val usado: Double,
    val limite: Double,
    val disponivel: Double,
    val usadoFormatado: String,
    val limiteFormatado: String,
    val disponivelFormatado: String,
    val detalhe: String
)

data class ParcelamentoUi(
    val id: Long,
    val descricao: String,
    val cartaoNome: String,
    val parcelaFormatada: String,
    val saldoDevedorFormatado: String,
    val progresso: Int,
    val resumo: String,
    val proximoVencimento: String
)

@HiltViewModel
class CarteiraViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val cartaoRepository: CartaoRepository,
    private val transacaoRepository: TransacaoRepository,
    private val parcelamentoRepository: ParcelamentoRepository
) : ViewModel() {

    private val _mensagens = MutableSharedFlow<String>()
    val mensagens = _mensagens.asSharedFlow()

    val uiState: StateFlow<CarteiraUiState> = combine(
        contaRepository.getAll(),
        cartaoRepository.getAll(),
        transacaoRepository.getAll(),
        parcelamentoRepository.getAll()
    ) { contas, cartoes, transacoes, parcelamentos ->
        CarteiraUiState(
            isLoading = false,
            contas = contas.map { conta -> conta.toUi(transacoes) },
            cartoes = cartoes.map { cartao -> cartao.toUi(transacoes, parcelamentos) },
            parcelamentos = parcelamentos
                .filter { it.parcelasPagas < it.totalParcelas }
                .sortedBy { it.proximaDataMillis() }
                .mapNotNull { parcelamento ->
                    val transacao = transacoes.firstOrNull {
                        it.id == parcelamento.transacaoPrincipalId
                    } ?: return@mapNotNull null
                    val cartaoNome = cartoes.firstOrNull { it.id == parcelamento.cartaoId }?.nome
                        ?: "Cartão removido"
                    parcelamento.toUi(transacao, cartaoNome)
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CarteiraUiState(isLoading = true)
    )

    private fun Conta.toUi(transacoes: List<Transacao>): ContaUi {
        val movimento = transacoes
            .filter { it.formaPagamento == "conta" && it.contaId == id }
            .sumOf { transacao -> if (transacao.tipo == "receita") transacao.valor else -transacao.valor }
        val saldoEstimado = saldoAtual + movimento
        return ContaUi(
            conta = this,
            id = id,
            nome = nome,
            tipo = tipo.replaceFirstChar { it.uppercase() },
            saldo = saldoEstimado,
            saldoFormatado = formatarValor(saldoEstimado),
            detalhe = if (ativa) "Conta ativa" else "Conta inativa"
        )
    }

    private fun Cartao.toUi(
        transacoes: List<Transacao>,
        parcelamentos: List<Parcelamento>
    ): CartaoUi {
        val despesasPorFatura = transacoes
            .filter { it.formaPagamento == "cartao" && it.cartaoId == id && !it.parcelado && it.tipo == "despesa" }
            .map { compra ->
                val competencia = RegrasFinanceiras.dataFatura(compra, this)
                competencia to compra
            }
            .groupBy({ it.first }, { it.second })
        val despesasAvulsas = despesasPorFatura.entries.sumOf { (competencia, compras) ->
            val totalCompras = compras.sumOf { it.valor }
            val totalPago = transacoes
                .filter {
                    RegrasFinanceiras.isPagamentoFatura(it) &&
                        it.cartaoId == id &&
                        RegrasFinanceiras.pagamentoPertenceAFatura(it, competencia)
                }
                .sumOf { it.valor }
            (totalCompras - totalPago).coerceAtLeast(0.0)
        }
        val saldoParcelado = parcelamentos
            .filter { it.cartaoId == id }
            .sumOf { it.valorParcela * (it.totalParcelas - it.parcelasPagas).coerceAtLeast(0) }
        val usado = despesasAvulsas + saldoParcelado
        val disponivel = limiteTotal - usado
        return CartaoUi(
            cartao = this,
            id = id,
            nome = nome,
            usado = usado,
            limite = limiteTotal,
            disponivel = disponivel,
            usadoFormatado = formatarValor(usado),
            limiteFormatado = formatarValor(limiteTotal),
            disponivelFormatado = formatarValor(disponivel),
            detalhe = "Fecha dia $diaFechamento - vence dia $diaVencimento"
        )
    }

    private fun formatarValor(valor: Double): String =
        "R$ %,.2f".format(valor)

    private fun Parcelamento.toUi(transacao: Transacao, cartaoNome: String): ParcelamentoUi {
        val restantes = (totalParcelas - parcelasPagas).coerceAtLeast(0)
        val saldoDevedor = valorParcela * restantes
        val proximaData = Calendar.getInstance().apply {
            timeInMillis = dataPrimeiraParcela
            add(Calendar.MONTH, parcelasPagas)
        }.timeInMillis
        return ParcelamentoUi(
            id = id,
            descricao = transacao.observacao?.takeIf { it.isNotBlank() } ?: "Compra parcelada",
            cartaoNome = cartaoNome,
            parcelaFormatada = formatarValor(valorParcela),
            saldoDevedorFormatado = formatarValor(saldoDevedor),
            progresso = if (totalParcelas == 0) 0 else {
                ((parcelasPagas.toFloat() / totalParcelas) * 100).toInt().coerceIn(0, 100)
            },
            resumo = "$parcelasPagas de $totalParcelas pagas - $restantes restantes",
            proximoVencimento = formatarData(proximaData)
        )
    }

    private fun Parcelamento.proximaDataMillis(): Long =
        Calendar.getInstance().apply {
            timeInMillis = dataPrimeiraParcela
            add(Calendar.MONTH, parcelasPagas)
        }.timeInMillis

    private fun formatarData(valor: Long): String {
        val data = Calendar.getInstance().apply { timeInMillis = valor }
        return "%02d/%02d/%04d".format(
            data.get(Calendar.DAY_OF_MONTH),
            data.get(Calendar.MONTH) + 1,
            data.get(Calendar.YEAR)
        )
    }

    fun salvarConta(contaAtual: Conta?, nome: String, tipo: String, saldo: String) {
        val nomeLimpo = nome.trim()
        val saldoValor = saldo.replace(",", ".").toDoubleOrNull()
        if (nomeLimpo.isBlank() || saldoValor == null) {
            emitirMensagem("Informe nome e saldo válidos.")
            return
        }
        viewModelScope.launch {
            if (contaAtual == null) {
                contaRepository.insert(
                    Conta(
                        nome = nomeLimpo,
                        tipo = tipo,
                        saldoInicial = saldoValor,
                        saldoAtual = saldoValor,
                        criadoEm = System.currentTimeMillis()
                    )
                )
            } else {
                val diferenca = saldoValor - contaAtual.saldoInicial
                contaRepository.update(
                    contaAtual.copy(
                        nome = nomeLimpo,
                        tipo = tipo,
                        saldoInicial = saldoValor,
                        saldoAtual = contaAtual.saldoAtual + diferenca
                    )
                )
            }
            _mensagens.emit("Conta salva.")
        }
    }

    fun excluirConta(item: ContaUi) {
        viewModelScope.launch {
            val possuiTransacao = transacaoRepository.getAll().first().any { it.contaId == item.id }
            if (possuiTransacao) {
                _mensagens.emit("Esta conta está em uso e não pode ser excluída.")
            } else {
                contaRepository.delete(item.conta)
                _mensagens.emit("Conta excluída.")
            }
        }
    }

    fun salvarCartao(
        cartaoAtual: Cartao?,
        nome: String,
        limite: String,
        fechamento: String,
        vencimento: String
    ) {
        val nomeLimpo = nome.trim()
        val limiteValor = limite.replace(",", ".").toDoubleOrNull()
        val fechamentoValor = fechamento.toIntOrNull()
        val vencimentoValor = vencimento.toIntOrNull()
        if (
            nomeLimpo.isBlank() ||
            limiteValor == null || limiteValor < 0 ||
            fechamentoValor !in 1..31 ||
            vencimentoValor !in 1..31
        ) {
            emitirMensagem("Confira nome, limite e dias do cartão.")
            return
        }
        val diaFechamento = requireNotNull(fechamentoValor)
        val diaVencimento = requireNotNull(vencimentoValor)
        viewModelScope.launch {
            val cartao = cartaoAtual?.copy(
                nome = nomeLimpo,
                limiteTotal = limiteValor,
                diaFechamento = diaFechamento,
                diaVencimento = diaVencimento
            ) ?: Cartao(
                nome = nomeLimpo,
                limiteTotal = limiteValor,
                diaFechamento = diaFechamento,
                diaVencimento = diaVencimento,
                criadoEm = System.currentTimeMillis()
            )
            if (cartaoAtual == null) cartaoRepository.insert(cartao) else cartaoRepository.update(cartao)
            _mensagens.emit("Cartão salvo.")
        }
    }

    fun excluirCartao(item: CartaoUi) {
        viewModelScope.launch {
            val possuiTransacao = transacaoRepository.getAll().first().any { it.cartaoId == item.id }
            val possuiParcelamento = parcelamentoRepository.getAll().first().any { it.cartaoId == item.id }
            if (possuiTransacao || possuiParcelamento) {
                _mensagens.emit("Este cartão está em uso e não pode ser excluído.")
            } else {
                cartaoRepository.delete(item.cartao)
                _mensagens.emit("Cartão excluído.")
            }
        }
    }

    private fun emitirMensagem(mensagem: String) {
        viewModelScope.launch { _mensagens.emit(mensagem) }
    }
}
