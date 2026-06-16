package com.example.financeiro.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.databinding.ItemContaOnboardingBinding
import com.example.financeiro.databinding.ItemCartaoOnboardingBinding
import com.example.financeiro.databinding.ItemParcelamentoOnboardingBinding

// ─── UI Models ────────────────────────────────────────────────────────────────

data class ContaItemUi(val index: Int, val nome: String, val tipo: String, val saldo: Double)
data class CartaoItemUi(val index: Int, val nome: String, val limite: Double, val diaFechamento: Int, val diaVencimento: Int)
data class ParcelamentoRascunhoUi(val index: Int, val descricao: String, val valorParcela: Double, val parcelasRestantes: Int, val cartaoNome: String)

// ─── ContaListAdapter ─────────────────────────────────────────────────────────

class ContaListAdapter(
    private val onRemover: (Int) -> Unit
) : ListAdapter<ContaItemUi, ContaListAdapter.ViewHolder>(ContaDiff()) {

    inner class ViewHolder(private val binding: ItemContaOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContaItemUi) {
            binding.tvNomeConta.text = item.nome
            binding.tvTipoConta.text = item.tipo
            binding.tvSaldoConta.text = "R$ %.2f".format(item.saldo)
            binding.btnRemover.setOnClickListener { onRemover(item.index) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContaOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ContaDiff : DiffUtil.ItemCallback<ContaItemUi>() {
        override fun areItemsTheSame(a: ContaItemUi, b: ContaItemUi) = a.index == b.index
        override fun areContentsTheSame(a: ContaItemUi, b: ContaItemUi) = a == b
    }
}

// ─── CartaoListAdapter ────────────────────────────────────────────────────────

class CartaoListAdapter(
    private val onRemover: (Int) -> Unit
) : ListAdapter<CartaoItemUi, CartaoListAdapter.ViewHolder>(CartaoDiff()) {

    inner class ViewHolder(private val binding: ItemCartaoOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartaoItemUi) {
            binding.tvNomeCartao.text = item.nome
            binding.tvLimiteCartao.text = "Limite: R$ %.2f".format(item.limite)
            binding.tvDiasCartao.text = "Fecha dia ${item.diaFechamento} · Vence dia ${item.diaVencimento}"
            binding.btnRemover.setOnClickListener { onRemover(item.index) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartaoOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class CartaoDiff : DiffUtil.ItemCallback<CartaoItemUi>() {
        override fun areItemsTheSame(a: CartaoItemUi, b: CartaoItemUi) = a.index == b.index
        override fun areContentsTheSame(a: CartaoItemUi, b: CartaoItemUi) = a == b
    }
}

// ─── ParcelamentoRascunhoAdapter ──────────────────────────────────────────────

class ParcelamentoRascunhoAdapter(
    private val onRemover: (Int) -> Unit
) : ListAdapter<ParcelamentoRascunhoUi, ParcelamentoRascunhoAdapter.ViewHolder>(ParcelamentoDiff()) {

    inner class ViewHolder(private val binding: ItemParcelamentoOnboardingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ParcelamentoRascunhoUi) {
            binding.tvDescricao.text = item.descricao
            binding.tvCartao.text = item.cartaoNome
            binding.tvParcelas.text = "${item.parcelasRestantes}x R$ %.2f".format(item.valorParcela)
            binding.tvTotal.text = "Total: R$ %.2f".format(item.valorParcela * item.parcelasRestantes)
            binding.btnRemover.setOnClickListener { onRemover(item.index) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParcelamentoOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ParcelamentoDiff : DiffUtil.ItemCallback<ParcelamentoRascunhoUi>() {
        override fun areItemsTheSame(a: ParcelamentoRascunhoUi, b: ParcelamentoRascunhoUi) = a.index == b.index
        override fun areContentsTheSame(a: ParcelamentoRascunhoUi, b: ParcelamentoRascunhoUi) = a == b
    }
}