package com.example.financeiro.ui.dashboard

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var faturaAdapter: FaturaCartaoAdapter
    private var ocultarSaldos = false

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
        ocultarSaldos = requireContext()
            .getSharedPreferences("financeiro_prefs", 0)
            .getBoolean("ocultar_saldos_dashboard", false)
        setupFaturas()
        setupBotoes()
        observeUiState()
        observeEventos()
    }

    private fun setupFaturas() {
        faturaAdapter = FaturaCartaoAdapter { item ->
            confirmarPagamentoFatura(item)
        }
        binding.recyclerFaturas.adapter = faturaAdapter
    }

    private fun setupBotoes() {
        binding.btnMesAnterior.setOnClickListener {
            viewModel.irParaMesAnterior()
        }
        binding.btnOcultarSaldos.setOnClickListener {
            ocultarSaldos = !ocultarSaldos
            requireContext()
                .getSharedPreferences("financeiro_prefs", 0)
                .edit()
                .putBoolean("ocultar_saldos_dashboard", ocultarSaldos)
                .apply()
            atualizarPrivacidadeSaldos()
            observeUiStateRender(viewModel.uiState.value)
        }
        binding.btnProximoMes.setOnClickListener {
            viewModel.irParaProximoMes()
        }
        binding.fabAdicionarTransacao.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_adicionar_transacao)
        }
        binding.btnVerTransacoes.setOnClickListener {
            val state = viewModel.uiState.value
            findNavController().navigate(
                R.id.action_dashboard_to_transacoes,
                bundleOf("mes" to state.mesAtual, "ano" to state.anoAtual)
            )
        }
        binding.btnContasCartoes.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_carteira)
        }
        binding.btnCategorias.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_categorias)
        }
        binding.btnRelatorios.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_relatorios)
        }
    }

    private fun observeUiStateRender(state: DashboardUiState) {
        binding.tvSaldoTenho.text = valorPrivado(state.saldoTenho)
        binding.tvSaldoAtual.text = valorPrivado(state.saldoAtual)
        binding.tvReceitas.text = valorPrivado(state.totalReceitas)
        binding.tvDespesas.text = valorPrivado(state.totalDespesas)
        atualizarPrivacidadeSaldos()
        faturaAdapter.ocultarValores = ocultarSaldos
        faturaAdapter.submitList(state.faturas)
    }

    private fun atualizarPrivacidadeSaldos() {
        binding.btnOcultarSaldos.setIconResource(
            if (ocultarSaldos) R.drawable.ic_visibility_off else R.drawable.ic_visibility
        )
        binding.btnOcultarSaldos.contentDescription =
            if (ocultarSaldos) "Mostrar saldos" else "Ocultar saldos"
    }

    private fun valorPrivado(valor: Double): String =
        if (ocultarSaldos) "******" else formatarValor(valor)

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

                    binding.tvLabelMes.text = state.labelMes
                    binding.btnProximoMes.isEnabled = state.podeIrParaProximoMes
                    binding.btnProximoMes.alpha = if (state.podeIrParaProximoMes) 1f else 0.3f

                    binding.layoutFaturas.visibility =
                        if (state.faturas.isEmpty()) View.GONE else View.VISIBLE
                    observeUiStateRender(state)
                }
            }
        }
    }

    private fun observeEventos() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventos.collect { evento ->
                    when (evento) {
                        is DashboardEvento.FaturaPaga -> {
                            val fatura = viewModel.uiState.value.faturas.firstOrNull {
                                evento.cartaoId == it.cartaoId
                            }
                            val mensagem = if (fatura != null && evento.valor + 0.009 >= fatura.valor) {
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

                        is DashboardEvento.Erro -> {
                            Snackbar.make(binding.root, evento.mensagem, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun confirmarPagamentoFatura(item: FaturaCartaoUi) {
        val state = viewModel.uiState.value
        val opcoes = state.contasPagamento.map { it.nome } + "Dinheiro"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (ocultarSaldos) "Pagar fatura" else "Pagar ${item.valorFormatado}")
            .setItems(opcoes.toTypedArray()) { _, which ->
                val conta = state.contasPagamento.getOrNull(which)
                if (conta != null) {
                    confirmarValorPagamento(item.cartaoId, item.valor, "conta", conta.id)
                } else {
                    confirmarValorPagamento(item.cartaoId, item.valor, "dinheiro", null)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarValorPagamento(
        cartaoId: Long,
        valorAberto: Double,
        formaPagamento: String,
        contaId: Long?
    ) {
        val campoValor = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Valor do pagamento"
            if (!ocultarSaldos) {
                setText("%.2f".format(valorAberto).replace(",", "."))
                selectAll()
            }
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
                    viewModel.pagarFatura(cartaoId, formaPagamento, contaId, valor)
                }
            }
            .show()
    }

    private fun formatarValor(valor: Double): String {
        return "R$ %,.2f".format(valor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
