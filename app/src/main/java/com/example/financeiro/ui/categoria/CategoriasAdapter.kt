package com.example.financeiro.ui.categoria

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.databinding.ItemCategoriaBinding
import com.example.financeiro.domain.model.Subcategoria

class CategoriasAdapter(
    private val onEditar: (CategoriaUi) -> Unit,
    private val onExcluir: (CategoriaUi) -> Unit,
    private val onAdicionarSubcategoria: (CategoriaUi) -> Unit,
    private val onEditarSubcategoria: (CategoriaUi, Subcategoria) -> Unit,
    private val onExcluirSubcategoria: (Subcategoria) -> Unit
) : ListAdapter<CategoriaUi, CategoriasAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoriaUi) {
            binding.tvNome.text = item.categoria.nome
            binding.tvUso.text = if (item.quantidadeTransacoes == 1) {
                "1 transacao"
            } else {
                "${item.quantidadeTransacoes} transacoes"
            }
            binding.btnEditar.setOnClickListener { onEditar(item) }
            binding.btnExcluir.setOnClickListener { onExcluir(item) }
            binding.btnAdicionarSubcategoria.setOnClickListener { onAdicionarSubcategoria(item) }
            binding.recyclerSubcategorias.adapter = SubcategoriasAdapter(
                onEditar = { onEditarSubcategoria(item, it) },
                onExcluir = onExcluirSubcategoria
            )
            (binding.recyclerSubcategorias.adapter as SubcategoriasAdapter).submitList(item.subcategorias)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemCategoriaBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<CategoriaUi>() {
        override fun areItemsTheSame(oldItem: CategoriaUi, newItem: CategoriaUi) =
            oldItem.categoria.id == newItem.categoria.id

        override fun areContentsTheSame(oldItem: CategoriaUi, newItem: CategoriaUi) =
            oldItem == newItem
    }
}

private class SubcategoriasAdapter(
    private val onEditar: (Subcategoria) -> Unit,
    private val onExcluir: (Subcategoria) -> Unit
) : ListAdapter<Subcategoria, SubcategoriasAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: com.example.financeiro.databinding.ItemSubcategoriaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Subcategoria) {
            binding.tvNome.text = item.nome
            binding.btnEditar.setOnClickListener { onEditar(item) }
            binding.btnExcluir.setOnClickListener { onExcluir(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            com.example.financeiro.databinding.ItemSubcategoriaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<Subcategoria>() {
        override fun areItemsTheSame(oldItem: Subcategoria, newItem: Subcategoria) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Subcategoria, newItem: Subcategoria) =
            oldItem == newItem
    }
}
