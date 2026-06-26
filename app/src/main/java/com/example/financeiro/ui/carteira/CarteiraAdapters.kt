package com.example.financeiro.ui.carteira

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.R
import com.example.financeiro.databinding.ItemCartaoCarteiraBinding
import com.example.financeiro.databinding.ItemContaCarteiraBinding

class ContaAdapter(
    private val onAbrir: (ContaUi) -> Unit,
    private val onEditar: (ContaUi) -> Unit,
    private val onExcluir: (ContaUi) -> Unit
) : ListAdapter<ContaUi, ContaAdapter.ViewHolder>(ContaDiff()) {

    inner class ViewHolder(private val binding: ItemContaCarteiraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContaUi) {
            binding.tvNomeConta.text = item.nome
            binding.tvTipoConta.text = "${item.tipo} - toque para ver extrato"
            binding.tvSaldoConta.text = item.saldoFormatado
            val cor = if (item.saldo >= 0) R.color.receita else R.color.despesa
            binding.tvSaldoConta.setTextColor(ContextCompat.getColor(binding.root.context, cor))
            binding.btnEditarConta.setOnClickListener { onEditar(item) }
            binding.btnExcluirConta.setOnClickListener { onExcluir(item) }
            binding.root.setOnClickListener { onAbrir(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContaCarteiraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContaDiff : DiffUtil.ItemCallback<ContaUi>() {
        override fun areItemsTheSame(oldItem: ContaUi, newItem: ContaUi): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ContaUi, newItem: ContaUi): Boolean = oldItem == newItem
    }
}

class CartaoAdapter(
    private val onAbrir: (CartaoUi) -> Unit,
    private val onEditar: (CartaoUi) -> Unit,
    private val onExcluir: (CartaoUi) -> Unit
) : ListAdapter<CartaoUi, CartaoAdapter.ViewHolder>(CartaoDiff()) {

    inner class ViewHolder(private val binding: ItemCartaoCarteiraBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartaoUi) {
            binding.tvNomeCartao.text = item.nome
            binding.tvDetalheCartao.text = "${item.detalhe} - toque para ver faturas"
            binding.tvUsadoCartao.text = "Usado: ${item.usadoFormatado}"
            binding.tvLimiteCartao.text = "Limite: ${item.limiteFormatado}"
            binding.tvDisponivelCartao.text = item.disponivelFormatado
            val progresso = if (item.limite <= 0.0) 0 else ((item.usado / item.limite) * 100).toInt().coerceIn(0, 100)
            binding.progressLimite.progress = progresso
            val corDisponivel = if (item.disponivel >= 0) R.color.receita else R.color.despesa
            binding.tvDisponivelCartao.setTextColor(ContextCompat.getColor(binding.root.context, corDisponivel))
            binding.btnEditarCartao.setOnClickListener { onEditar(item) }
            binding.btnExcluirCartao.setOnClickListener { onExcluir(item) }
            binding.root.setOnClickListener { onAbrir(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCartaoCarteiraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CartaoDiff : DiffUtil.ItemCallback<CartaoUi>() {
        override fun areItemsTheSame(oldItem: CartaoUi, newItem: CartaoUi): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CartaoUi, newItem: CartaoUi): Boolean = oldItem == newItem
    }
}
