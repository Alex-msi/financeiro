package com.example.financeiro.ui.categoria

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.financeiro.databinding.FragmentCategoriasBinding
import com.example.financeiro.domain.model.Subcategoria
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoriasFragment : Fragment() {

    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriasViewModel by viewModels()
    private lateinit var adapter: CategoriasAdapter
    private var tipoAtual = "despesa"
    private var categoriaSelecionadaId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarLista()
        configurarAcoes()
        observarEstado()
    }

    private fun configurarLista() {
        adapter = CategoriasAdapter(
            onEditar = { abrirCategoria(it) },
            onExcluir = { confirmarExclusaoCategoria(it) },
            onAdicionarSubcategoria = { abrirSubcategoria(it, null) },
            onEditarSubcategoria = { categoria, subcategoria ->
                abrirSubcategoria(categoria, subcategoria)
            },
            onExcluirSubcategoria = { confirmarExclusaoSubcategoria(it) }
        )
        binding.recyclerCategorias.adapter = adapter
    }

    private fun configurarAcoes() {
        binding.btnVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.btnDespesas.setOnClickListener {
            categoriaSelecionadaId = null
            viewModel.selecionarTipo("despesa")
        }
        binding.btnReceitas.setOnClickListener {
            categoriaSelecionadaId = null
            viewModel.selecionarTipo("receita")
        }
        binding.autoCompleteCategoria.setOnClickListener {
            binding.autoCompleteCategoria.showDropDown()
        }
        binding.btnAdicionar.setOnClickListener { abrirCategoria(null) }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        tipoAtual = state.tipo
                        atualizarSeletorCategorias(state.categorias)
                        binding.tvVazio.isVisible = state.categorias.isEmpty()
                        binding.layoutSelecionarCategoria.isVisible = state.categorias.isNotEmpty()
                        binding.btnDespesas.isChecked = state.tipo == "despesa"
                        binding.btnReceitas.isChecked = state.tipo == "receita"
                    }
                }
                launch {
                    viewModel.mensagens.collect {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun atualizarSeletorCategorias(categorias: List<CategoriaUi>) {
        val selecionada = categorias.firstOrNull { it.categoria.id == categoriaSelecionadaId }
            ?: categorias.firstOrNull()
        categoriaSelecionadaId = selecionada?.categoria?.id

        binding.autoCompleteCategoria.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categorias.map { it.categoria.nome }
            )
        )
        binding.autoCompleteCategoria.setText(selecionada?.categoria?.nome.orEmpty(), false)
        adapter.submitList(selecionada?.let(::listOf).orEmpty())

        binding.autoCompleteCategoria.setOnItemClickListener { _, _, position, _ ->
            val item = categorias[position]
            categoriaSelecionadaId = item.categoria.id
            binding.autoCompleteCategoria.setText(item.categoria.nome, false)
            adapter.submitList(listOf(item))
        }
    }

    private fun abrirCategoria(item: CategoriaUi?) {
        abrirDialogoTexto(
            titulo = if (item == null) "Nova categoria" else "Editar categoria",
            valorAtual = item?.categoria?.nome.orEmpty()
        ) { nome ->
            viewModel.salvarCategoria(item?.categoria?.id, nome, item?.categoria?.tipo ?: tipoAtual)
        }
    }

    private fun abrirSubcategoria(item: CategoriaUi, subcategoria: Subcategoria?) {
        abrirDialogoTexto(
            titulo = if (subcategoria == null) {
                "Nova subcategoria de ${item.categoria.nome}"
            } else {
                "Editar subcategoria"
            },
            valorAtual = subcategoria?.nome.orEmpty()
        ) { nome ->
            viewModel.salvarSubcategoria(item.categoria.id, subcategoria?.id, nome)
        }
    }

    private fun abrirDialogoTexto(
        titulo: String,
        valorAtual: String,
        aoSalvar: (String) -> Unit
    ) {
        val campo = EditText(requireContext()).apply {
            setText(valorAtual)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSelection(text.length)
            hint = "Nome"
            val margem = (24 * resources.displayMetrics.density).toInt()
            setPadding(margem, paddingTop, margem, paddingBottom)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setView(campo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar") { _, _ -> aoSalvar(campo.text.toString()) }
            .show()
    }

    private fun confirmarExclusaoCategoria(item: CategoriaUi) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir categoria?")
            .setMessage("As subcategorias de ${item.categoria.nome} também serão excluídas.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> viewModel.excluirCategoria(item) }
            .show()
    }

    private fun confirmarExclusaoSubcategoria(item: Subcategoria) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Excluir subcategoria?")
            .setMessage(item.nome)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ -> viewModel.excluirSubcategoria(item) }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
