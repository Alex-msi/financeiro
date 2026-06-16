package com.example.financeiro.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentOnboardingContasBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class OnboardingContasFragment : Fragment() {

    private var _binding: FragmentOnboardingContasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var contaAdapter: ContaListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingContasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTipoDropdown()
        setupRecyclerView()
        setupBotoes()
        observeUiState()
    }

    private fun setupTipoDropdown() {
        val tipos = listOf("Conta Corrente", "Poupança", "Dinheiro", "Investimento", "Outro")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        binding.autoCompleteTipo.setAdapter(adapter)
        binding.autoCompleteTipo.setText(tipos[0], false)
    }

    private fun setupRecyclerView() {
        contaAdapter = ContaListAdapter(onRemover = { index ->
            viewModel.removerConta(index)
        })
        binding.recyclerContas.adapter = contaAdapter
    }

    private fun setupBotoes() {
        binding.btnAdicionarConta.setOnClickListener {
            val nome = binding.editNomeConta.text?.toString() ?: ""
            val tipo = binding.autoCompleteTipo.text?.toString() ?: "Conta Corrente"
            val saldoStr = binding.editSaldoConta.text?.toString() ?: "0"
            val saldo = saldoStr.replace(",", ".").toDoubleOrNull() ?: 0.0

            viewModel.adicionarConta(nome, tipo, saldo)
            limparFormulario()
        }

        binding.btnContinuar.setOnClickListener {
            if (viewModel.podeContinuarContas()) {
                viewModel.irParaProximoPasso()
            } else {
                Snackbar.make(binding.root, "Adicione ao menos uma conta para continuar.", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnVoltar.setOnClickListener {
            viewModel.irParaPassoAnterior()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    contaAdapter.submitList(state.contas.mapIndexed { index, conta ->
                        ContaItemUi(index, conta.nome, conta.tipo, conta.saldoAtual)
                    })
                    binding.emptyState.isVisible = state.contas.isEmpty()
                    binding.recyclerContas.isVisible = state.contas.isNotEmpty()

                    state.erro?.let { erro ->
                        Snackbar.make(binding.root, erro, Snackbar.LENGTH_SHORT).show()
                        viewModel.limparErro()
                    }
                }
            }
        }
    }

    private fun limparFormulario() {
        binding.editNomeConta.setText("")
        binding.editSaldoConta.setText("")
        binding.editNomeConta.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}