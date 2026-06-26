package com.example.financeiro.ui.extrato

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.R
import com.example.financeiro.databinding.ItemLancamentoExtratoBinding

class ItensExtratoAdapter :
    ListAdapter<ItemExtratoUi, ItensExtratoAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: ItemLancamentoExtratoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ItemExtratoUi) {
            binding.tvDescricao.text = item.descricao
            binding.tvCategoria.text = item.categoria
            binding.tvData.text = item.data
            binding.tvTipo.text = if (item.isCredito) "Credito" else "Debito"
            binding.tvValor.text = if (item.isCredito) {
                "+ ${item.valorFormatado}"
            } else {
                "- ${item.valorFormatado}"
            }
            val cor = if (item.isCredito) R.color.receita else R.color.despesa
            binding.tvValor.setTextColor(ContextCompat.getColor(binding.root.context, cor))
            binding.tvTipo.setTextColor(ContextCompat.getColor(binding.root.context, cor))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemLancamentoExtratoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<ItemExtratoUi>() {
        override fun areItemsTheSame(oldItem: ItemExtratoUi, newItem: ItemExtratoUi) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ItemExtratoUi, newItem: ItemExtratoUi) =
            oldItem == newItem
    }
}
