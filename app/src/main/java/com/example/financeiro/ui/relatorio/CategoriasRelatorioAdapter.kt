package com.example.financeiro.ui.relatorio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.databinding.ItemCategoriaRelatorioBinding

class CategoriasRelatorioAdapter :
    ListAdapter<CategoriaRelatorioUi, CategoriasRelatorioAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: ItemCategoriaRelatorioBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoriaRelatorioUi) {
            binding.tvNome.text = item.nome
            binding.tvValor.text = item.valorFormatado
            binding.tvPercentual.text = "%.1f%%".format(item.percentual)
            binding.progressCategoria.progress = item.percentual.toInt().coerceIn(0, 100)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemCategoriaRelatorioBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<CategoriaRelatorioUi>() {
        override fun areItemsTheSame(oldItem: CategoriaRelatorioUi, newItem: CategoriaRelatorioUi) =
            oldItem.nome == newItem.nome

        override fun areContentsTheSame(oldItem: CategoriaRelatorioUi, newItem: CategoriaRelatorioUi) =
            oldItem == newItem
    }
}
