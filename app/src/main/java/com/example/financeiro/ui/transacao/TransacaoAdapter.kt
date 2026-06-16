package com.example.financeiro.ui.transacao

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.financeiro.R
import com.example.financeiro.databinding.ItemTransacaoBinding

// ─── Adapter ─────────────────────────────────────────────────────────────────

class TransacaoAdapter(
    private val onClique: (TransacaoItemUi) -> Unit,
    private val onDeletar: (Long) -> Unit
) : ListAdapter<TransacaoItemUi, TransacaoAdapter.ViewHolder>(TransacaoDiff()) {

    inner class ViewHolder(private val binding: ItemTransacaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransacaoItemUi) {
            binding.tvDescricao.text = item.descricao
            binding.tvData.text = item.dataFormatada
            binding.tvCategoria.text = item.categoriaNome
            binding.tvValor.text = item.valorFormatado

            val corValor = if (item.isReceita)
                ContextCompat.getColor(binding.root.context, R.color.receita)
            else
                ContextCompat.getColor(binding.root.context, R.color.despesa)
            binding.tvValor.setTextColor(corValor)

            // Prefixo visual: + para receita, - para despesa
            binding.tvValor.text = if (item.isReceita) "+ ${item.valorFormatado}"
            else "- ${item.valorFormatado}"

            // Indicador lateral de tipo
            val corIndicador = if (item.isReceita)
                ContextCompat.getColor(binding.root.context, R.color.receita)
            else
                ContextCompat.getColor(binding.root.context, R.color.despesa)
            binding.viewIndicador.setBackgroundColor(corIndicador)

            binding.root.setOnClickListener { onClique(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransacaoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    fun getItemAtPosition(position: Int): TransacaoItemUi = getItem(position)

    class TransacaoDiff : DiffUtil.ItemCallback<TransacaoItemUi>() {
        override fun areItemsTheSame(a: TransacaoItemUi, b: TransacaoItemUi) = a.id == b.id
        override fun areContentsTheSame(a: TransacaoItemUi, b: TransacaoItemUi) = a == b
    }
}

// ─── SwipeToDelete ────────────────────────────────────────────────────────────

/**
 * Configura swipe para esquerda com fundo vermelho e ícone de lixeira.
 * Chame attachToRecyclerView() após criar.
 */
class SwipeToDeleteCallback(
    private val adapter: TransacaoAdapter,
    private val onDeletar: (Long) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val paintFundo = Paint().apply { color = Color.parseColor("#C62828") }
    private val paintTexto = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        val item = adapter.getItemAtPosition(position)
        onDeletar(item.id)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val fundo = RectF(
            itemView.right + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        c.drawRoundRect(fundo, 12f, 12f, paintFundo)
        c.drawText(
            "Deletar",
            itemView.right - 180f,
            itemView.top + (itemView.height / 2f) + 15f,
            paintTexto
        )
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}

/**
 * Extensão para facilitar o attach do swipe ao RecyclerView.
 * Uso: recyclerView.adicionarSwipeParaDeletar(adapter) { id -> viewModel.deletar(id) }
 */
fun RecyclerView.adicionarSwipeParaDeletar(
    adapter: TransacaoAdapter,
    onDeletar: (Long) -> Unit
) {
    val callback = SwipeToDeleteCallback(adapter, onDeletar)
    ItemTouchHelper(callback).attachToRecyclerView(this)
}