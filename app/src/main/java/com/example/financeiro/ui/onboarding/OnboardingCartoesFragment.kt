package com.example.financeiro.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.financeiro.databinding.FragmentOnboardingCartoesBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class OnboardingCartoesFragment : Fragment() {

    private var _binding: FragmentOnboardingCartoesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    private lateinit var cartaoAdapter: CartaoListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingCartoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBotoes()
        observeUiState()
    }

    private fun setupRecyclerView() {
        cartaoAdapter = CartaoListAdapter(onRemover = { index ->
            viewModel.removerCartao(index)
        })
        binding.recyclerCartoes.adapter = cartaoAdapter
    }

    private fun setupBotoes() {
        binding.btnAdicionarCartao.setOnClickListener {
            val nome = binding.editNomeCartao.text?.toString() ?: ""
            val limiteStr = binding.editLimite.text?.toString() ?: "0"
            val limite = limiteStr.replace(",", ".").toDoubleOrNull() ?: 0.0
            val fechamento = binding.editDiaFechamento.text?.toString()?.toIntOrNull() ?: 0
            val vencimento = binding.editDiaVencimento.text?.toString()?.toIntOrNull() ?: 0

            viewModel.adicionarCartao(nome, limite, fechamento, vencimento)
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
                    cartaoAdapter.submitList(state.cartoes.mapIndexed { index, cartao ->
                        CartaoItemUi(index, cartao.nome, cartao.limiteTotal, cartao.diaFechamento, cartao.diaVencimento)
                    })
                    binding.emptyState.isVisible = state.cartoes.isEmpty()
                    binding.recyclerCartoes.isVisible = state.cartoes.isNotEmpty()

                    state.erro?.let { erro ->
                        Snackbar.make(binding.root, erro, Snackbar.LENGTH_SHORT).show()
                        viewModel.limparErro()
                    }
                }
            }
        }
    }

    private fun limparFormulario() {
        binding.editNomeCartao.setText("")
        binding.editLimite.setText("")
        binding.editDiaFechamento.setText("")
        binding.editDiaVencimento.setText("")
        binding.editNomeCartao.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}