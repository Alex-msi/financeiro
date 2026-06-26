package com.example.financeiro.ui.fatura

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.databinding.FragmentFaturaCartaoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FaturaCartaoFragment : Fragment() {

    private var _binding: FragmentFaturaCartaoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FaturaCartaoViewModel by viewModels()
    private val adapter = ItensFaturaAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaturaCartaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerLancamentos.adapter = adapter
        binding.btnVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.btnMesAnterior.setOnClickListener { viewModel.irParaMesAnterior() }
        binding.btnProximoMes.setOnClickListener { viewModel.irParaProximoMes() }
        binding.btnPagarFatura.setOnClickListener { confirmarPagamento() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.contentGroup.isVisible = !state.isLoading
                    binding.tvNomeCartao.text = state.nomeCartao
                    binding.tvMes.text = state.labelMes
                    binding.tvPeriodo.text = state.periodo
                    binding.tvTotal.text = state.totalFormatado
                    binding.tvTotalPago.text = state.totalPagoFormatado
                    binding.layoutTotalPago.isVisible = state.totalPago > 0.0
                    binding.tvTotalAberto.text = state.totalAbertoFormatado
                    binding.btnPagarFatura.isVisible = state.podePagar
                    binding.tvVazio.isVisible = state.isEmpty
                    binding.recyclerLancamentos.isVisible = !state.isEmpty
                    adapter.submitList(state.itens)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventos.collect { evento ->
                    when (evento) {
                        is FaturaCartaoEvento.FaturaPaga -> {
                            val state = viewModel.uiState.value
                            val mensagem = if (evento.valor + 0.009 >= state.totalAberto) {
                                "Fatura quitada: ${formatarValor(evento.valor)}"
                            } else {
                                "Pagamento registrado: ${formatarValor(evento.valor)}"
                            }
                            Snackbar.make(
                                binding.root,
                                mensagem,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        is FaturaCartaoEvento.Erro -> {
                            Snackbar.make(binding.root, evento.mensagem, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarPagamento() {
        val state = viewModel.uiState.value
        val opcoes = state.contasPagamento.map { it.nome } + "Dinheiro"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pagar ${state.totalAbertoFormatado}")
            .setItems(opcoes.toTypedArray()) { _, which ->
                val conta = state.contasPagamento.getOrNull(which)
                if (conta != null) {
                    confirmarValorPagamento("conta", conta.id)
                } else {
                    confirmarValorPagamento("dinheiro", null)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarValorPagamento(formaPagamento: String, contaId: Long?) {
        val state = viewModel.uiState.value
        val campoValor = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("%.2f".format(state.totalAberto).replace(",", "."))
            selectAll()
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Valor do pagamento")
            .setView(campoValor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Pagar") { _, _ ->
                val valor = campoValor.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
                if (valor == null || valor <= 0.0) {
                    Snackbar.make(binding.root, "Informe um valor válido.", Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.pagarFatura(formaPagamento, contaId, valor)
                }
            }
            .show()
    }

    private fun formatarValor(valor: Double): String =
        "R$ %,.2f".format(valor)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
