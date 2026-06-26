package com.example.financeiro.ui.carteira

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.databinding.FragmentCarteiraBinding
import com.example.financeiro.databinding.DialogCartaoBinding
import com.example.financeiro.databinding.DialogContaBinding
import com.example.financeiro.domain.model.Cartao
import com.example.financeiro.domain.model.Conta
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CarteiraFragment : Fragment() {

    private var _binding: FragmentCarteiraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarteiraViewModel by viewModels()
    private lateinit var contasAdapter: ContaAdapter
    private lateinit var cartoesAdapter: CartaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarteiraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListas()
        setupBotoes()
        observeUiState()
    }

    private fun setupListas() {
        contasAdapter = ContaAdapter(
            onAbrir = { abrirExtrato(it.id) },
            onEditar = { abrirConta(it.conta) },
            onExcluir = { confirmarExclusaoConta(it) }
        )
        cartoesAdapter = CartaoAdapter(
            onAbrir = { abrirFaturas(it.id) },
            onEditar = { abrirCartao(it.cartao) },
            onExcluir = { confirmarExclusaoCartao(it) }
        )
        binding.recyclerContas.adapter = contasAdapter
        binding.recyclerCartoes.adapter = cartoesAdapter
    }

    private fun setupBotoes() {
        binding.btnVoltar.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnAdicionarConta.setOnClickListener { abrirConta(null) }
        binding.btnAdicionarCartao.setOnClickListener { abrirCartao(null) }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.contentGroup.isVisible = !state.isLoading
                    binding.tvEmptyState.isVisible = false

                    binding.sectionContas.isVisible = true
                    binding.sectionCartoes.isVisible = true
                    contasAdapter.submitList(state.contas)
                    cartoesAdapter.submitList(state.cartoes)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mensagens.collect {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirFaturas(cartaoId: Long) {
        findNavController().navigate(
            com.example.financeiro.R.id.action_carteira_to_fatura,
            Bundle().apply { putLong("cartaoId", cartaoId) }
        )
    }

    private fun abrirExtrato(contaId: Long) {
        findNavController().navigate(
            com.example.financeiro.R.id.action_carteira_to_extrato,
            Bundle().apply { putLong("contaId", contaId) }
        )
    }

    private fun abrirConta(conta: Conta?) {
        val dialogBinding = DialogContaBinding.inflate(layoutInflater)
        val tipos = listOf("Conta Corrente", "Poupanca", "Dinheiro", "Investimento", "Outro")
        dialogBinding.editTipo.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tipos)
        )
        dialogBinding.editNome.setText(conta?.nome.orEmpty())
        dialogBinding.editTipo.setText(conta?.tipo ?: tipos.first(), false)
        dialogBinding.editSaldo.setText(conta?.saldoInicial?.toString().orEmpty())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (conta == null) "Nova conta" else "Editar conta")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                viewModel.salvarConta(
                    conta,
                    dialogBinding.editNome.text?.toString().orEmpty(),
                    dialogBinding.editTipo.text?.toString().orEmpty(),
                    dialogBinding.editSaldo.text?.toString().orEmpty()
                )
            }
            .show()
    }

    private fun abrirCartao(cartao: Cartao?) {
        val dialogBinding = DialogCartaoBinding.inflate(layoutInflater)
        dialogBinding.editNome.setText(cartao?.nome.orEmpty())
        dialogBinding.editLimite.setText(cartao?.limiteTotal?.toString().orEmpty())
        dialogBinding.editFechamento.setText(cartao?.diaFechamento?.toString().orEmpty())
        dialogBinding.editVencimento.setText(cartao?.diaVencimento?.toString().orEmpty())

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (cartao == null) "Novo cartao" else "Editar cartao")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ ->
                viewModel.salvarCartao(
                    cartao,
                    dialogBinding.editNome.text?.toString().orEmpty(),
                    dialogBinding.editLimite.text?.toString().orEmpty(),
                    dialogBinding.editFechamento.text?.toString().orEmpty(),
                    dialogBinding.editVencimento.text?.toString().orEmpty()
                )
            }
            .show()
    }

    private fun confirmarExclusaoConta(item: ContaUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir conta?")
            .setMessage(item.nome)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> viewModel.excluirConta(item) }
            .show()
    }

    private fun confirmarExclusaoCartao(item: CartaoUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir cartao?")
            .setMessage(item.nome)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> viewModel.excluirCartao(item) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
