package com.example.financeiro.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBotoes()
        observeUiState()
    }

    private fun setupBotoes() {
        binding.btnMesAnterior.setOnClickListener {
            viewModel.irParaMesAnterior()
        }
        binding.btnProximoMes.setOnClickListener {
            viewModel.irParaProximoMes()
        }
        binding.btnVerTransacoes.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_transacoes)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentGroup.visibility = View.GONE
                        return@collect
                    }

                    binding.progressBar.visibility = View.GONE
                    binding.contentGroup.visibility = View.VISIBLE

                    // Saldo Tenho
                    binding.tvSaldoTenho.text = formatarValor(state.saldoTenho)

                    // Navegação de mês
                    binding.tvLabelMes.text = state.labelMes
                    binding.btnProximoMes.isEnabled = state.podeIrParaProximoMes
                    binding.btnProximoMes.alpha = if (state.podeIrParaProximoMes) 1f else 0.3f

                    // Cards do mês
                    binding.tvReceitas.text = formatarValor(state.totalReceitas)
                    binding.tvDespesas.text = formatarValor(state.totalDespesas)
                    binding.tvSaldoMes.text = formatarValor(state.saldoMes)

                    // Cor do saldo do mês: verde se positivo, vermelho se negativo
                    val corSaldo = if (state.saldoMes >= 0)
                        ContextCompat.getColor(requireContext(), R.color.receita)
                    else
                        ContextCompat.getColor(requireContext(), R.color.despesa)
                    binding.tvSaldoMes.setTextColor(corSaldo)
                }
            }
        }
    }

    private fun formatarValor(valor: Double): String {
        return "R$ %,.2f".format(valor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
