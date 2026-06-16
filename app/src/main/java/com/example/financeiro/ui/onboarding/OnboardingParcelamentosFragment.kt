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
import com.example.financeiro.databinding.FragmentOnboardingParcelamentosBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class OnboardingParcelamentosFragment : Fragment() {

    private var _binding: FragmentOnboardingParcelamentosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var parcelamentoAdapter: ParcelamentoRascunhoAdapter

    // Cartões disponíveis para seleção (carregados do estado)
    private var cartaoSelecionadoId: Long = -1L
    private var cartaoSelecionadoNome: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingParcelamentosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBotoes()
        observeUiState()
    }

    private fun setupRecyclerView() {
        parcelamentoAdapter = ParcelamentoRascunhoAdapter(onRemover = { index ->
            viewModel.removerParcelamentoRascunho(index)
        })
        binding.recyclerParcelamentos.adapter = parcelamentoAdapter
    }

    private fun setupCartoesDropdown(cartoes: List<com.example.financeiro.domain.model.Cartao>) {
        val nomes = cartoes.map { it.nome }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, nomes)
        binding.autoCompleteCartao.setAdapter(adapter)

        if (cartoes.isNotEmpty()) {
            binding.autoCompleteCartao.setText(cartoes[0].nome, false)
            cartaoSelecionadoId = cartoes[0].id
            cartaoSelecionadoNome = cartoes[0].nome
        }

        binding.autoCompleteCartao.setOnItemClickListener { _, _, position, _ ->
            cartaoSelecionadoId = cartoes[position].id
            cartaoSelecionadoNome = cartoes[position].nome
        }
    }

    private fun setupBotoes() {
        binding.btnAdicionarParcelamento.setOnClickListener {
            if (cartaoSelecionadoId == -1L) {
                Snackbar.make(binding.root, "Você precisa ter ao menos um cartão cadastrado.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val descricao = binding.editDescricao.text?.toString() ?: ""
            val valorStr = binding.editValorParcela.text?.toString() ?: "0"
            val valor = valorStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val parcelas = binding.editParcelasRestantes.text?.toString()?.toIntOrNull() ?: 0

            viewModel.adicionarParcelamentoRascunho(descricao, valor, parcelas, cartaoSelecionadoId, cartaoSelecionadoNome)
            limparFormulario()
        }

        binding.btnContinuar.setOnClickListener {
            viewModel.irParaProximoPasso()
        }

        binding.btnPular.setOnClickListener {
            viewModel.pularPasso()
        }

        binding.btnVoltar.setOnClickListener {
            viewModel.irParaPassoAnterior()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Atualiza dropdown de cartões
                    if (state.cartoes.isNotEmpty()) {
                        setupCartoesDropdown(state.cartoes)
                        binding.layoutCartao.isVisible = true
                        binding.tvSemCartoes.isVisible = false
                    } else {
                        binding.layoutCartao.isVisible = false
                        binding.tvSemCartoes.isVisible = true
                    }

                    parcelamentoAdapter.submitList(state.parcelamentosPendentes.mapIndexed { index, p ->
                        ParcelamentoRascunhoUi(index, p.descricao, p.valorParcela, p.parcelasRestantes, p.cartaoNome)
                    })

                    binding.emptyState.isVisible = state.parcelamentosPendentes.isEmpty()
                    binding.recyclerParcelamentos.isVisible = state.parcelamentosPendentes.isNotEmpty()

                    state.erro?.let { erro ->
                        Snackbar.make(binding.root, erro, Snackbar.LENGTH_SHORT).show()
                        viewModel.limparErro()
                    }
                }
            }
        }
    }

    private fun limparFormulario() {
        binding.editDescricao.setText("")
        binding.editValorParcela.setText("")
        binding.editParcelasRestantes.setText("")
        binding.editDescricao.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}