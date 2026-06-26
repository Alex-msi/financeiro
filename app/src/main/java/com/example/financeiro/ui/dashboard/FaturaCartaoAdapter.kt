package com.example.financeiro.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.databinding.ItemFaturaCartaoBinding

class FaturaCartaoAdapter(
    private val onPagar: (FaturaCartaoUi) -> Unit
) : ListAdapter<FaturaCartaoUi, FaturaCartaoAdapter.ViewHolder>(FaturaCartaoDiff()) {

    inner class ViewHolder(private val binding: ItemFaturaCartaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FaturaCartaoUi) {
            binding.tvNomeCartao.text = item.nomeCartao
            binding.tvValorFatura.text = item.valorFormatado
            binding.tvResumoParcelas.text = item.resumoParcelas
            binding.btnPagarFatura.isVisible = item.valor > 0.0
            binding.btnPagarFatura.setOnClickListener { onPagar(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFaturaCartaoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FaturaCartaoDiff : DiffUtil.ItemCallback<FaturaCartaoUi>() {
        override fun areItemsTheSame(oldItem: FaturaCartaoUi, newItem: FaturaCartaoUi): Boolean =
            oldItem.cartaoId == newItem.cartaoId

        override fun areContentsTheSame(oldItem: FaturaCartaoUi, newItem: FaturaCartaoUi): Boolean =
            oldItem == newItem
    }
}
