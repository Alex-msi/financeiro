package com.example.financeiro.ui.transacao

import android.app.DatePickerDialog
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
import androidx.navigation.fragment.navArgs
import com.example.financeiro.databinding.FragmentAdicionarTransacaoBinding
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
        setupBotoes()
        observeUiState()
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
        binding.autoCompleteConta.setOnClickListener { binding.autoCompleteConta.showDropDown() }
        binding.autoCompleteCartao.setOnClickListener { binding.autoCompleteCartao.showDropDown() }
        binding.autoCompleteCategoria.setOnClickListener { binding.autoCompleteCategoria.showDropDown() }

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

    private fun setupBotoes() {
        binding.btnSalvar.setOnClickListener { viewModel.salvar() }
        binding.btnCancelar.setOnClickListener { findNavController().popBackStack() }
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
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
                    if (binding.editData.text.toString() != state.dataFormatada) {
                        binding.editData.setText(state.dataFormatada)
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

                    if (binding.checkParcelado.isChecked != state.parcelado) {
                        binding.checkParcelado.isChecked = state.parcelado
                    }
                    if (binding.editNumeroParcelas.text.toString() != state.numeroParcelas) {
                        binding.editNumeroParcelas.setText(state.numeroParcelas)
                    }

                    // Dropdown categorias
                    atualizarDropdownCategorias(state)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
