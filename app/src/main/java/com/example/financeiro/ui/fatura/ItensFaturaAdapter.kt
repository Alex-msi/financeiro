package com.example.financeiro.ui.fatura

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.R
import com.example.financeiro.databinding.ItemLancamentoFaturaBinding

class ItensFaturaAdapter :
    ListAdapter<ItemFaturaUi, ItensFaturaAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: ItemLancamentoFaturaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ItemFaturaUi) {
            binding.tvDescricao.text = item.descricao
            binding.tvCategoria.text = item.categoria
            binding.tvData.text = item.data
            binding.tvValor.text = item.valorFormatado
            binding.tvDetalhe.text = item.detalhe
            binding.tvValor.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (item.pagamento) R.color.receita else R.color.despesa
                )
            )
            binding.tvPago.isVisible = true
            binding.tvPago.text = when {
                item.pagamento -> "Pagamento"
                item.pago -> "Pago"
                item.pagavel -> "Aberto"
                else -> "Aguardando"
            }
            binding.tvPago.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    when {
                        item.pagamento -> R.color.receita
                        item.pago -> R.color.receita
                        item.pagavel -> R.color.despesa
                        else -> R.color.on_surface_variant
                    }
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemLancamentoFaturaBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<ItemFaturaUi>() {
        override fun areItemsTheSame(oldItem: ItemFaturaUi, newItem: ItemFaturaUi) =
            oldItem.chave == newItem.chave

        override fun areContentsTheSame(oldItem: ItemFaturaUi, newItem: ItemFaturaUi) =
            oldItem == newItem
    }
}
