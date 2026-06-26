package com.example.financeiro.ui.categoria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeiro.domain.model.Categoria
import com.example.financeiro.domain.model.Subcategoria
import com.example.financeiro.domain.repository.CategoriaRepository
import com.example.financeiro.domain.repository.TransacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriaUi(
    val categoria: Categoria,
    val subcategorias: List<Subcategoria>,
    val quantidadeTransacoes: Int
)

data class CategoriasUiState(
    val tipo: String = "despesa",
    val categorias: List<CategoriaUi> = emptyList()
)

@HiltViewModel
class CategoriasViewModel @Inject constructor(
    private val categoriaRepository: CategoriaRepository,
    private val transacaoRepository: TransacaoRepository
) : ViewModel() {

    private val tipoSelecionado = MutableStateFlow("despesa")
    private val _mensagens = MutableSharedFlow<String>()
    val mensagens = _mensagens.asSharedFlow()

    val uiState: StateFlow<CategoriasUiState> = combine(
        categoriaRepository.getAll(),
        categoriaRepository.getAllSubcategorias(),
        transacaoRepository.getAll(),
        tipoSelecionado
    ) { categorias, subcategorias, transacoes, tipo ->
        CategoriasUiState(
            tipo = tipo,
            categorias = categorias
                .filter { it.tipo == tipo }
                .map { categoria ->
                    CategoriaUi(
                        categoria = categoria,
                        subcategorias = subcategorias.filter { it.categoriaId == categoria.id },
                        quantidadeTransacoes = transacoes.count { it.categoriaId == categoria.id }
                    )
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriasUiState()
    )

    init {
        viewModelScope.launch {
            if (categoriaRepository.getAll().first().isEmpty()) {
                criarCategoriasIniciais()
            }
        }
    }

    fun selecionarTipo(tipo: String) {
        tipoSelecionado.value = tipo
    }

    fun salvarCategoria(id: Long?, nome: String, tipo: String) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isBlank()) {
            viewModelScope.launch { _mensagens.emit("Informe o nome da categoria.") }
            return
        }
        viewModelScope.launch {
            val existente = id?.let { categoriaRepository.getById(it) }
            if (existente == null) {
                categoriaRepository.insert(
                    Categoria(nome = nomeLimpo, tipo = tipo, criadoEm = System.currentTimeMillis())
                )
            } else {
                categoriaRepository.update(existente.copy(nome = nomeLimpo, tipo = tipo))
            }
            _mensagens.emit("Categoria salva.")
        }
    }

    fun excluirCategoria(item: CategoriaUi) {
        viewModelScope.launch {
            if (item.quantidadeTransacoes > 0) {
                _mensagens.emit("Esta categoria está em uso e não pode ser excluída.")
            } else {
                categoriaRepository.delete(item.categoria)
                _mensagens.emit("Categoria excluída.")
            }
        }
    }

    fun salvarSubcategoria(categoriaId: Long, id: Long?, nome: String) {
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isBlank()) {
            viewModelScope.launch { _mensagens.emit("Informe o nome da subcategoria.") }
            return
        }
        viewModelScope.launch {
            val existente = id?.let { categoriaRepository.getSubcategoriaById(it) }
            if (existente == null) {
                categoriaRepository.insertSubcategoria(
                    Subcategoria(
                        nome = nomeLimpo,
                        categoriaId = categoriaId,
                        criadoEm = System.currentTimeMillis()
                    )
                )
            } else {
                categoriaRepository.updateSubcategoria(existente.copy(nome = nomeLimpo))
            }
            _mensagens.emit("Subcategoria salva.")
        }
    }

    fun excluirSubcategoria(subcategoria: Subcategoria) {
        viewModelScope.launch {
            val emUso = transacaoRepository.getAll().first().any { it.subcategoriaId == subcategoria.id }
            if (emUso) {
                _mensagens.emit("Esta subcategoria está em uso e não pode ser excluída.")
            } else {
                categoriaRepository.deleteSubcategoria(subcategoria)
                _mensagens.emit("Subcategoria excluída.")
            }
        }
    }

    private suspend fun criarCategoriasIniciais() {
        val despesas = listOf(
            "Alimentação" to listOf("Açougue", "Padaria", "Restaurante", "Supermercado"),
            "Animal de Estimação" to listOf("Ração", "Remédio pet", "Veterinário"),
            "Casa" to listOf("Água", "Aluguel", "Condomínio", "Internet", "Luz"),
            "Educação" to listOf("Cursos", "Escola", "Faculdade", "Material Escolar"),
            "Gastos Pessoais" to listOf("Academia", "Perfumaria", "Roupas e Calçados"),
            "Impostos" to listOf("IPTU", "IPVA", "IR", "Taxas"),
            "Lazer" to listOf("Cinema", "Passeios", "Viagens"),
            "Saúde" to listOf("Dentista", "Exames", "Farmácia", "Médico"),
            "Seguros" to listOf("Seguro do Carro", "Seguro Residencial", "Seguro de Vida"),
            "Serviços Financeiros" to listOf("Anuidade Cartão", "Juros", "Tarifas bancárias"),
            "Transporte" to listOf("Combustível", "Estacionamento", "Manutenção", "Transporte público"),
            "Outros Gastos" to listOf("Diversos")
        )
        despesas.forEach { (nome, subs) ->
            val categoriaId = categoriaRepository.insert(
                Categoria(nome = nome, tipo = "despesa", criadoEm = System.currentTimeMillis())
            )
            subs.forEach { nomeSub ->
                categoriaRepository.insertSubcategoria(
                    Subcategoria(
                        nome = nomeSub,
                        categoriaId = categoriaId,
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }
        }

        val receitaId = categoriaRepository.insert(
            Categoria(nome = "Receita", tipo = "receita", criadoEm = System.currentTimeMillis())
        )
        listOf("13º Salário", "Bônus", "Comissão", "Outras Receitas", "Reembolso", "Salário")
            .forEach { nome ->
                categoriaRepository.insertSubcategoria(
                    Subcategoria(
                        nome = nome,
                        categoriaId = receitaId,
                        criadoEm = System.currentTimeMillis()
                    )
                )
            }
    }
}
