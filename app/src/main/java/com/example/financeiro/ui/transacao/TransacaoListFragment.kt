package com.example.financeiro.ui.transacao

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentTransacaoListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TransacaoListFragment : Fragment() {

    private var _binding: FragmentTransacaoListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransacaoListViewModel by viewModels()
    private lateinit var adapter: TransacaoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransacaoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBotoes()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = TransacaoAdapter(
            onClique = { item ->
                confirmarExclusao(item)
                // Navegação para edição implementada na S17
            },
            onDeletar = { id -> deletarTransacao(id) }
        )
        binding.recyclerTransacoes.adapter = adapter
        binding.recyclerTransacoes.adicionarSwipeParaDeletar(adapter) { id ->
            deletarTransacao(id)
        }
    }

    private fun confirmarExclusao(item: TransacaoItemUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir transacao")
            .setMessage("Deseja excluir \"${item.descricao}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                deletarTransacao(item.id)
            }
            .show()
    }

    private fun deletarTransacao(id: Long) {
        viewModel.deletarTransacao(id)
        Snackbar.make(binding.root, "Transacao excluida", Snackbar.LENGTH_SHORT).show()
    }

    private fun setupBotoes() {
        binding.btnMesAnterior.setOnClickListener { viewModel.irParaMesAnterior() }
        binding.btnProximoMes.setOnClickListener { viewModel.irParaProximoMes() }

        // FAB para adicionar transação — navegação implementada na S17
        binding.fabAdicionarTransacao.setOnClickListener {
            findNavController().navigate(R.id.action_lista_to_adicionar)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.recyclerTransacoes.isVisible = !state.isLoading && !state.isEmpty
                    binding.tvEmptyState.isVisible = state.isEmpty

                    binding.tvLabelMes.text = state.labelMes
                    binding.btnProximoMes.isEnabled = state.podeIrParaProximoMes
                    binding.btnProximoMes.alpha = if (state.podeIrParaProximoMes) 1f else 0.3f

                    adapter.submitList(state.transacoes)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
