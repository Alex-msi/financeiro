package com.example.financeiro.ui.carteira

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.databinding.ItemParcelamentoCarteiraBinding

class ParcelamentoAdapter :
    ListAdapter<ParcelamentoUi, ParcelamentoAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(private val binding: ItemParcelamentoCarteiraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ParcelamentoUi) {
            binding.tvDescricao.text = item.descricao
            binding.tvCartao.text = item.cartaoNome
            binding.tvParcela.text = "${item.parcelaFormatada} por parcela"
            binding.tvResumo.text = item.resumo
            binding.tvProximoVencimento.text = "Proxima: ${item.proximoVencimento}"
            binding.tvSaldoDevedor.text = "Falta ${item.saldoDevedorFormatado}"
            binding.progressParcelamento.progress = item.progresso
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemParcelamentoCarteiraBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<ParcelamentoUi>() {
        override fun areItemsTheSame(oldItem: ParcelamentoUi, newItem: ParcelamentoUi) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ParcelamentoUi, newItem: ParcelamentoUi) =
            oldItem == newItem
    }
}
