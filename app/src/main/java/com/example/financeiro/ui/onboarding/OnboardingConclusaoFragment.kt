package com.example.financeiro.ui.onboarding

import android.content.Context
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
import com.example.financeiro.databinding.FragmentOnboardingConclusaoBinding
import kotlinx.coroutines.launch

class OnboardingConclusaoFragment : Fragment() {

    private var _binding: FragmentOnboardingConclusaoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingConclusaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUiState()

        binding.btnConcluir.setOnClickListener {
            viewModel.concluirOnboarding {
                salvarPrefsOnboarding()
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
                    // Resumo
                    binding.tvResumoContas.text = "${state.contas.size} conta(s) cadastrada(s)"
                    binding.tvResumoCartoes.text = "${state.cartoes.size} cartão(ões) cadastrado(s)"
                    binding.tvResumoParcelamentos.text =
                        if (state.parcelamentosPendentes.isEmpty()) "Nenhum parcelamento em andamento"
                        else "${state.parcelamentosPendentes.size} parcelamento(s) importado(s)"

                    val saldoTotal = state.contas.sumOf { it.saldoAtual }
                    binding.tvResumoSaldo.text = "Saldo inicial: R$ %.2f".format(saldoTotal)

                    // Loading
                    binding.progressBar.isVisible = state.isLoading
                    binding.btnConcluir.isEnabled = !state.isLoading
                }
            }
        }
    }

    private fun salvarPrefsOnboarding() {
        val prefs = requireContext().getSharedPreferences("saldo_tenho_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completo", true).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}