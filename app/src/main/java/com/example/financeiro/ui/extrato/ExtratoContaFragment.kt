package com.example.financeiro.ui.extrato

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentExtratoContaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExtratoContaFragment : Fragment() {

    private var _binding: FragmentExtratoContaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExtratoContaViewModel by viewModels()
    private val adapter = ItensExtratoAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExtratoContaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerLancamentos.adapter = adapter
        binding.btnVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.btnMesAnterior.setOnClickListener { viewModel.irParaMesAnterior() }
        binding.btnProximoMes.setOnClickListener { viewModel.irParaProximoMes() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.contentGroup.isVisible = !state.isLoading
                    binding.tvNomeConta.text = state.nomeConta
                    binding.tvMes.text = state.labelMes
                    binding.tvSaldoAnterior.text = state.saldoAnteriorFormatado
                    binding.tvCreditos.text = state.creditosFormatados
                    binding.tvDebitos.text = state.debitosFormatados
                    binding.tvSaldoFinal.text = state.saldoFinalFormatado
                    binding.tvSaldoFinal.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (state.saldoFinal >= 0) R.color.receita else R.color.despesa
                        )
                    )
                    binding.tvVazio.isVisible = state.isEmpty
                    binding.recyclerLancamentos.isVisible = !state.isEmpty
                    adapter.submitList(state.itens)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
