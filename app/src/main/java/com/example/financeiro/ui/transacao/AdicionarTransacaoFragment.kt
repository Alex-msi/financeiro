package com.example.financeiro.ui.transacao

import android.app.DatePickerDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.databinding.FragmentAdicionarTransacaoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class AdicionarTransacaoFragment : Fragment() {

    private var _binding: FragmentAdicionarTransacaoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdicionarTransacaoViewModel by viewModels()

    // Safe Args — transacaoId = -1L significa nova transação
    // Descomente quando o nav_graph tiver o argumento declarado (S21):
    // private val args: AdicionarTransacaoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdicionarTransacaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carrega transação para edição se id válido:
        // val id = args.transacaoId
        // if (id != -1L) viewModel.carregarTransacao(id)

        setupCampos()
        configurarRolagemComTeclado()
        setupBotoes()
        observeUiState()
        carregarTransacaoSeNecessario()
    }

    // ─── Setup dos campos ─────────────────────────────────────────────────────

    private fun setupCampos() {
        // Valor
        binding.editValor.doAfterTextChanged { text ->
            viewModel.onValorChanged(text?.toString() ?: "")
        }

        // Observação
        binding.editObservacao.doAfterTextChanged { text ->
            viewModel.onObservacaoChanged(text?.toString() ?: "")
        }

        binding.checkParcelado.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onParceladoChanged(isChecked)
        }
        binding.editNumeroParcelas.doAfterTextChanged { text ->
            viewModel.onNumeroParcelasChanged(text?.toString() ?: "")
        }
        binding.editParcelasPagas.doAfterTextChanged { text ->
            viewModel.onParcelasPagasChanged(text?.toString() ?: "")
        }
        binding.checkRecorrente.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onRecorrenteChanged(isChecked)
        }
        binding.editQuantidadeRecorrencias.doAfterTextChanged { text ->
            viewModel.onQuantidadeRecorrenciasChanged(text?.toString() ?: "")
        }
        binding.autoCompleteConta.setOnClickListener { binding.autoCompleteConta.showDropDown() }
        binding.autoCompleteCartao.setOnClickListener { binding.autoCompleteCartao.showDropDown() }
        binding.autoCompleteCategoria.setOnClickListener { binding.autoCompleteCategoria.showDropDown() }
        binding.autoCompleteSubcategoria.setOnClickListener { binding.autoCompleteSubcategoria.showDropDown() }

        // Data — abre DatePickerDialog
        binding.editData.setOnClickListener { abrirDatePicker() }
        binding.layoutData.setEndIconOnClickListener { abrirDatePicker() }

        // Tipo: Receita / Despesa (ToggleButton)
        binding.toggleReceita.setOnClickListener { viewModel.onTipoChanged("receita") }
        binding.toggleDespesa.setOnClickListener { viewModel.onTipoChanged("despesa") }

        // Forma de pagamento
        binding.toggleConta.setOnClickListener { viewModel.onFormaPagamentoChanged("conta") }
        binding.toggleCartao.setOnClickListener { viewModel.onFormaPagamentoChanged("cartao") }
        binding.toggleDinheiro.setOnClickListener { viewModel.onFormaPagamentoChanged("dinheiro") }
    }

    private fun configurarRolagemComTeclado() {
        val campos = listOf<View>(
            binding.editObservacao,
            binding.editValor,
            binding.editData,
            binding.autoCompleteCategoria,
            binding.autoCompleteSubcategoria,
            binding.autoCompleteConta,
            binding.autoCompleteCartao,
            binding.editNumeroParcelas,
            binding.editParcelasPagas,
            binding.editQuantidadeRecorrencias
        )
        campos.forEach { campo ->
            campo.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) rolarAteCampo(view)
            }
            campo.setOnClickListener {
                rolarAteCampo(campo)
                when (campo) {
                    binding.autoCompleteConta -> binding.autoCompleteConta.showDropDown()
                    binding.autoCompleteCartao -> binding.autoCompleteCartao.showDropDown()
                    binding.autoCompleteCategoria -> binding.autoCompleteCategoria.showDropDown()
                    binding.autoCompleteSubcategoria -> binding.autoCompleteSubcategoria.showDropDown()
                    binding.editData -> abrirDatePicker()
                }
            }
        }
    }

    private fun rolarAteCampo(campo: View) {
        binding.scrollFormulario.postDelayed({
            val rect = Rect()
            campo.getDrawingRect(rect)
            binding.scrollFormulario.offsetDescendantRectToMyCoords(campo, rect)
            binding.scrollFormulario.smoothScrollTo(0, (rect.top - 96).coerceAtLeast(0))
        }, 220)
    }

    private fun setupBotoes() {
        binding.btnSalvar.setOnClickListener { salvarComEscopoSeNecessario() }
        binding.btnCancelar.setOnClickListener { findNavController().popBackStack() }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun salvarComEscopoSeNecessario() {
        val state = viewModel.uiState.value
        if (!state.podeEscolherEscopoRecorrencia) {
            viewModel.salvar()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Salvar alteração")
            .setItems(arrayOf("Somente esta transação", "Esta e próximas")) { _, which ->
                val escopo = if (which == 0) {
                    EscopoEdicaoRecorrencia.SOMENTE_ESTA
                } else {
                    EscopoEdicaoRecorrencia.ESTA_E_PROXIMAS
                }
                viewModel.salvar(escopo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ─── Observação do estado ─────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Título da toolbar
                    binding.toolbar.title = state.titulo

                    // Loading
                    binding.progressBar.isVisible = state.isLoading
                    binding.btnSalvar.isEnabled = !state.isLoading

                    // Data
                    if (binding.editValor.text.toString() != state.valor) {
                        binding.editValor.setText(state.valor)
                    }
                    if (binding.editData.text.toString() != state.dataFormatada) {
                        binding.editData.setText(state.dataFormatada)
                    }
                    if (binding.editObservacao.text.toString() != state.observacao) {
                        binding.editObservacao.setText(state.observacao)
                    }

                    // Toggle tipo
                    binding.toggleReceita.isChecked = state.tipo == "receita"
                    binding.toggleDespesa.isChecked = state.tipo == "despesa"

                    // Toggle forma de pagamento
                    binding.toggleConta.isChecked = state.formaPagamento == "conta"
                    binding.toggleCartao.isChecked = state.formaPagamento == "cartao"
                    binding.toggleDinheiro.isChecked = state.formaPagamento == "dinheiro"

                    // Ocultar cartão se tipo = receita
                    binding.toggleCartao.isVisible = state.tipo == "despesa"

                    // Mostrar/ocultar selects de conta e cartão
                    binding.layoutSelecionarConta.isVisible = state.mostrarConta
                    binding.layoutSelecionarCartao.isVisible = state.mostrarCartao
                    binding.layoutParcelamento.isVisible = state.mostrarParcelamento
                    binding.layoutNumeroParcelas.isVisible = state.mostrarNumeroParcelas
                    binding.layoutParcelasPagas.isVisible = state.mostrarParcelasPagas
                    binding.layoutRecorrencia.isVisible = state.mostrarRecorrencia
                    binding.layoutQuantidadeRecorrencias.isVisible = state.mostrarQuantidadeRecorrencias
                    binding.layoutValor.hint = if (state.mostrarNumeroParcelas) {
                        "Valor total da compra (R$)"
                    } else {
                        "Valor (R$)"
                    }

                    if (binding.checkParcelado.isChecked != state.parcelado) {
                        binding.checkParcelado.isChecked = state.parcelado
                    }
                    if (binding.editNumeroParcelas.text.toString() != state.numeroParcelas) {
                        binding.editNumeroParcelas.setText(state.numeroParcelas)
                    }
                    if (binding.editParcelasPagas.text.toString() != state.parcelasPagas) {
                        binding.editParcelasPagas.setText(state.parcelasPagas)
                    }
                    if (binding.checkRecorrente.isChecked != state.recorrente) {
                        binding.checkRecorrente.isChecked = state.recorrente
                    }
                    if (binding.editQuantidadeRecorrencias.text.toString() != state.quantidadeRecorrencias) {
                        binding.editQuantidadeRecorrencias.setText(state.quantidadeRecorrencias)
                    }

                    // Dropdown categorias
                    atualizarDropdownCategorias(state)
                    atualizarDropdownSubcategorias(state)

                    // Dropdown contas
                    atualizarDropdownContas(state)

                    // Dropdown cartões
                    atualizarDropdownCartoes(state)

                    // Erro
                    state.erro?.let { erro ->
                        Snackbar.make(binding.root, erro, Snackbar.LENGTH_SHORT).show()
                        viewModel.limparErro()
                    }

                    // Salvo com sucesso → volta
                    if (state.salvoCom) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    // ─── Dropdowns ────────────────────────────────────────────────────────────

    private fun atualizarDropdownCategorias(state: AdicionarTransacaoUiState) {
        val categoriasFiltradas = state.categorias.filter { it.tipo == state.tipo }
        val nomes = categoriasFiltradas.map { it.nome }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomes)
        binding.autoCompleteCategoria.setAdapter(adapter)

        if (state.categoriaSelecionadaNome.isNotBlank()) {
            binding.autoCompleteCategoria.setText(state.categoriaSelecionadaNome, false)
        } else if (binding.autoCompleteCategoria.text.isNotBlank()) {
            binding.autoCompleteCategoria.setText("", false)
        }

        binding.autoCompleteCategoria.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCategoriaSelected(categoriasFiltradas[position])
        }
    }

    private fun atualizarDropdownContas(state: AdicionarTransacaoUiState) {
        val nomes = state.contas.map { it.nome }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomes)
        binding.autoCompleteConta.setAdapter(adapter)

        if (state.contaSelecionadaNome.isNotBlank()) {
            binding.autoCompleteConta.setText(state.contaSelecionadaNome, false)
        } else if (binding.autoCompleteConta.text.isNotBlank()) {
            binding.autoCompleteConta.setText("", false)
        }

        binding.autoCompleteConta.setOnItemClickListener { _, _, position, _ ->
            viewModel.onContaSelected(state.contas[position])
        }
    }

    private fun atualizarDropdownCartoes(state: AdicionarTransacaoUiState) {
        val nomes = state.cartoes.map { it.nome }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomes)
        binding.autoCompleteCartao.setAdapter(adapter)

        if (state.cartaoSelecionadoNome.isNotBlank()) {
            binding.autoCompleteCartao.setText(state.cartaoSelecionadoNome, false)
        } else if (binding.autoCompleteCartao.text.isNotBlank()) {
            binding.autoCompleteCartao.setText("", false)
        }

        binding.autoCompleteCartao.setOnItemClickListener { _, _, position, _ ->
            viewModel.onCartaoSelected(state.cartoes[position])
        }
    }

    // ─── DatePicker ───────────────────────────────────────────────────────────

    private fun abrirDatePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = viewModel.uiState.value.dataCompetencia
        }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val novaData = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.onDataChanged(novaData)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun atualizarDropdownSubcategorias(state: AdicionarTransacaoUiState) {
        val subcategorias = state.subcategorias.filter {
            it.categoriaId == state.categoriaSelecionadaId && it.ativa
        }
        binding.layoutSubcategoria.isVisible = state.categoriaSelecionadaId != null
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            subcategorias.map { it.nome }
        )
        binding.autoCompleteSubcategoria.setAdapter(adapter)

        if (state.subcategoriaSelecionadaNome.isNotBlank()) {
            binding.autoCompleteSubcategoria.setText(state.subcategoriaSelecionadaNome, false)
        } else if (binding.autoCompleteSubcategoria.text.isNotBlank()) {
            binding.autoCompleteSubcategoria.setText("", false)
        }

        binding.autoCompleteSubcategoria.setOnItemClickListener { _, _, position, _ ->
            viewModel.onSubcategoriaSelected(subcategorias[position])
        }
    }

    private fun carregarTransacaoSeNecessario() {
        val id = arguments?.getLong("transacaoId", -1L) ?: -1L
        if (id != -1L) {
            viewModel.carregarTransacao(id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
